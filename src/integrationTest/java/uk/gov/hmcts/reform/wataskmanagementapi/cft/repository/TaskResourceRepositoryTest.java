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
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TaskResourceRepositoryTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    private final TaskResource task = createTask("8d6cc5cf-c973-11eb-bdba-0242ac11001e", ExecutionType.MANUAL);

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.insert(task));
    }

    @Test
    void given_task_does_not_exists_then_insert() {
        transactionHelper.doInNewTransaction(() -> {
            TaskResource newId = createTask("new id", ExecutionType.BUILT_IN);
            taskResourceRepository.insert(newId);
        });
    }

    @Test
    void given_task_exists_then_fails() {
        taskResourceRepository.insert(task);
    }

    @Test
    void given_task_is_locked_then_other_transactions_cannot_make_changes() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> requireLockForGivenTask(task));

        Future<?> futureResult = executorService.submit(() -> {
            await().timeout(3, TimeUnit.SECONDS);
            requireLockForGivenTask(task);
            task.setAssignee("changed assignee");
            taskResourceRepository.insert(task);
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

    private void requireLockForGivenTask(TaskResource task) {
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.findById(task.getTaskId()));
    }

    @Test
    void shouldReadTaskData() {
        assertEquals(1, taskResourceRepository.count());

        final Iterable<TaskResource> tasksIt = taskResourceRepository.findAll();

        final TaskResource taskResource = tasksIt.iterator().next();
        final List<NoteResource> notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskResource.getTaskId()),
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
        String[] expectedAuthorizations = {"SPECIFIC", "BASIC"};

        assertAll(
            () -> assertNotNull(taskRole.getTaskRoleId()),
            () -> assertEquals("8d6cc5cf-c973-11eb-bdba-0242ac11001e", taskRole.getTaskId()),
            () -> assertTrue(taskRole.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRole.getRoleName()),
            () -> assertArrayEquals(expectedAuthorizations, taskRole.getAuthorizations())
        );
    }

    private TaskResource createTask(String taskId, ExecutionType executionCode) {
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
            new ExecutionTypeResource(executionCode, "Manual", "Manual Description"),
            "workType",
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
                new String[]{"SPECIFIC", "BASIC"},
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
