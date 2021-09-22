package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TaskResourceRepositoryTest extends SpringBootIntegrationBaseTest {

    private final TaskResource task = createTask();
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        task = createTask(taskId);
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(task));
    }

    @Test
    void given_task_is_locked_then_other_transactions_cannot_make_changes() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> requireLockForGivenTask(task));

        Future<?> futureResult = executorService.submit(() -> {
            await().timeout(3, TimeUnit.SECONDS);
            requireLockForGivenTask(task);
            task.setAssignee("changed assignee");
            taskResourceRepository.save(task);
        });

        await()
            .ignoreException(AssertionError.class)
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(7, TimeUnit.SECONDS)
            .until(() -> {
                Exception exception = Assertions.assertThrows(Exception.class, futureResult::get);
                log.info(exception.toString());
                assertThat(exception).hasMessageContaining("PessimisticLockException");

                return true;
            });

        transactionHelper.doInNewTransaction(() -> {
            TaskResource dbTask = taskResourceRepository.getByTaskId(task.getTaskId()).orElseThrow();
            assertEquals("someAssignee", dbTask.getAssignee());

        });
    }

    @Test
    void shouldReadTaskData() {
        String taskId = "8d6cc5cf-c973-11eb-bdba-0242ac11001e";

        assertEquals(1, taskResourceRepository.count());

        assertTrue(taskResourceRepository.findById(taskId).isPresent());
        WorkTypeResource workTypeResource = taskResourceRepository.findById(taskId).get().getWorkTypeResource();
        assertEquals("routine_work", workTypeResource.getId());
        assertEquals("Routine work", workTypeResource.getLabel());

        final Iterable<TaskResource> tasksIt = taskResourceRepository.findAll();

        final TaskResource taskResource = tasksIt.iterator().next();
        final List<NoteResource> notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals(taskId, taskResource.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, taskResource.getExecutionTypeCode().getExecutionCode()),
            () -> assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification()),
            () -> assertEquals(CFTTaskState.COMPLETED, taskResource.getState()),
            () -> assertEquals(TaskSystem.SELF, taskResource.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, taskResource.getBusinessContext()),
            () -> assertEquals(LocalDate.of(2022, 05, 9), taskResource.getAssignmentExpiry().toLocalDate()),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.get(0).getNoteType())
        );

        final Set<TaskRoleResource> taskRoles = taskResource.getTaskRoleResources();
        assertEquals(1, taskRoles.size());

        final TaskRoleResource taskRole = taskRoles.iterator().next();
        List<String> expectedAuthorizations = asList("SPECIFIC", "BASIC");

        assertAll(
            () -> assertNotNull(taskRole.getTaskRoleId()),
            () -> assertEquals(taskId, taskRole.getTaskId()),
            () -> assertTrue(taskRole.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRole.getRoleName()),
            () -> assertEquals(expectedAuthorizations, taskRole.getAuthorizations())
        );
    }

    private void requireLockForGivenTask(TaskResource task) {
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.findById(task.getTaskId()));
    }

    private TaskResource createTask() {
        List<NoteResource> notes = singletonList(
            new NoteResource("someCode",
                "noteTypeVal",
                "userVal", OffsetDateTime.now(),
                "someContent"
            ));
        return new TaskResource(
            taskId,
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            notes,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(new TaskRoleResource(
                "tribunal-caseofficer",
                true,
                false,
                false,
                false,
                false,
                false,
                asList("SPECIFIC", "BASIC"),
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
            )),
            "caseCategory"
        );
    }
}
