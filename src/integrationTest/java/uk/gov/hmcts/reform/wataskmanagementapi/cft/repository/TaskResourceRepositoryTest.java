package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TaskResourceRepositoryTest extends SpringBootIntegrationBaseTest {

    private String taskId;
    private TaskResource task;
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
    void given_insertAndLock_call_when_concurrent_calls_for_same_task_id_then_fail() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        TaskResource taskResource = new TaskResource(
            "some task id",
            "some task name",
            "some task type",
            CFTTaskState.ASSIGNED
        );

        executorService.execute(() -> {
            transactionHelper
                .doInNewTransaction(() -> taskResourceRepository.insertAndLock(taskResource.getTaskId()));
            await().timeout(10, TimeUnit.SECONDS);
            transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(taskResource));
        });

        await().timeout(3, TimeUnit.SECONDS); // to ensure the first call is processed first
        assertThrows(
            DataIntegrityViolationException.class,
            () -> transactionHelper
                .doInNewTransaction(() -> taskResourceRepository.insertAndLock(taskResource.getTaskId()))
        );
        checkTaskWasSaved(taskResource.getTaskId());

        executorService.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(13, TimeUnit.SECONDS);
    }

    @Test
    void given_insertAndLock_call_when_concurrent_calls_for_different_task_id_then_succeed()
        throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        TaskResource taskResource = new TaskResource(
            "some task id",
            "some task name",
            "some task type",
            CFTTaskState.ASSIGNED
        );

        executorService.execute(() -> {
            taskResourceRepository.insertAndLock(taskResource.getTaskId());
            await().timeout(10, TimeUnit.SECONDS);
            taskResourceRepository.save(taskResource);
        });

        TaskResource otherTaskResource = new TaskResource(
            "other task id",
            "other task name",
            "other task type",
            CFTTaskState.ASSIGNED
        );

        assertDoesNotThrow(() -> taskResourceRepository.insertAndLock(otherTaskResource.getTaskId()));
        checkTaskWasSaved(taskResource.getTaskId());
        checkTaskWasSaved(otherTaskResource.getTaskId());

        executorService.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(13, TimeUnit.SECONDS);
    }

    private void checkTaskWasSaved(String taskId) {
        assertEquals(
            taskId,
            taskResourceRepository.getByTaskId(taskId).orElseThrow().getTaskId()
        );
    }

    @Test
    void given_task_is_saved_when_findById_then_task_has_expected_fields() {
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
            () -> assertEquals(
                LocalDate.of(2022, 5, 9),
                taskResource.getAssignmentExpiry().toLocalDate()
            ),
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

    private TaskResource createTask(String taskId) {
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
