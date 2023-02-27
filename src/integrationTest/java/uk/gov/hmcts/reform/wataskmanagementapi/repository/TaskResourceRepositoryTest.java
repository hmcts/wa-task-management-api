package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TaskResourceRepositoryTest extends SpringBootIntegrationBaseTest {

    public static final Map<String, String> ADDITIONAL_PROPERTIES = Map.of(
        "name1", "value1",
        "name2", "value2",
        "name3", "value3"
    );

    private String taskId;
    private TaskResource task;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    @Autowired
    private TaskRoleResourceRepository taskRoleResourceRepository;

    @AfterEach
    void tearDown() {
        taskRoleResourceRepository.deleteAll();
        taskResourceRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        task = createTask(taskId);
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(task));
    }

    @Test
    @Timeout(value = 15, unit = SECONDS)
    @Execution(ExecutionMode.CONCURRENT)
    void should_insert_and_lock_when_concurrent_calls_for_different_task_id_then_succeed()
        throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        OffsetDateTime created = OffsetDateTime.parse("2022-05-08T20:15:45.345875+01:00");
        OffsetDateTime dueDate = OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00");
        OffsetDateTime priorityDate = OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00");

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "some task name",
            "some task type",
            CFTTaskState.ASSIGNED,
            created,
            dueDate,
            priorityDate
        );
        taskResource.setCreated(created);

        executorService.execute(() -> {
            taskResourceRepository.insertAndLock(
                taskResource.getTaskId(),
                taskResource.getCreated(),
                taskResource.getDueDateTime(),
                taskResource.getPriorityDate()
            );
            await().timeout(10, SECONDS);
            taskResourceRepository.save(taskResource);
        });

        TaskResource otherTaskResource = new TaskResource(
            "other task id",
            "other task name",
            "other task type",
            CFTTaskState.ASSIGNED,
            created,
            dueDate,
            priorityDate
        );

        assertDoesNotThrow(() -> taskResourceRepository.insertAndLock(
            otherTaskResource.getTaskId(),
            otherTaskResource.getCreated(),
            otherTaskResource.getDueDateTime(),
            otherTaskResource.getPriorityDate()
        ));

        await()
            .atMost(10, SECONDS)
            .untilAsserted(() -> {
                checkTaskWasSaved(taskResource.getTaskId());
                checkTaskWasSaved(otherTaskResource.getTaskId());
            });

        executorService.shutdown();
        //noinspection ResultOfMethodCallIgnored
        executorService.awaitTermination(13, SECONDS);

        assertAll(
            () -> assertTrue(executorService.isShutdown()),
            () -> assertTrue(executorService.isTerminated())
        );
    }

    @Test
    void given_task_is_created_when_find_by_id_then_task_roles_and_work_types_have_expected_values() {

        TaskResource createdTask = createTask(taskId);
        assertThat(createdTask.getTaskId()).isEqualTo(taskId);

        final TaskResource taskResource =
            taskResourceRepository.findById(taskId)
                .orElseThrow(
                    () -> new ResourceNotFoundException("Couldn't find the Task created using the id: " + taskId)
                );

        WorkTypeResource workTypeResource = taskResource.getWorkTypeResource();

        assertEquals("routine_work", workTypeResource.getId());
        assertEquals("Routine work", workTypeResource.getLabel());

        final List<NoteResource> notes = taskResource.getNotes();

        assertAll(
            () -> assertEquals(taskId, taskResource.getTaskId()),
            () -> assertEquals(ExecutionType.MANUAL, taskResource.getExecutionTypeCode().getExecutionCode()),
            () -> assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification()),
            () -> assertEquals(CFTTaskState.ASSIGNED, taskResource.getState()),
            () -> assertEquals(TaskSystem.SELF, taskResource.getTaskSystem()),
            () -> assertEquals(BusinessContext.CFT_TASK, taskResource.getBusinessContext()),
            () -> assertEquals(ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties()),
            () -> assertEquals(
                LocalDate.of(2022, 5, 9),
                taskResource.getAssignmentExpiry().toLocalDate()
            ),
            () -> assertNotNull(notes),
            () -> assertEquals("noteTypeVal", notes.get(0).getNoteType())
        );

        final List<TaskRoleResource> taskRoleResources =
            taskRoleResourceRepository.findByTaskId(createdTask.getTaskId());

        assertThat(taskRoleResources).isNotEmpty();
        assertThat(taskRoleResources).hasSize(1);

        final TaskRoleResource taskRoleResource = taskRoleResources.get(0);

        String[] expectedAuthorizations = new String[]{"SPECIFIC", "STANDARD"};

        assertAll(
            () -> assertNotNull(taskRoleResource.getTaskRoleId()),
            () -> assertEquals(taskId, taskRoleResource.getTaskId()),
            () -> assertTrue(taskRoleResource.getRead()),
            () -> assertEquals("tribunal-caseofficer", taskRoleResource.getRoleName()),
            () -> assertArrayEquals(expectedAuthorizations, taskRoleResource.getAuthorizations())
        );

        assertFalse(taskRoleResource.getComplete());
        assertFalse(taskRoleResource.getCompleteOwn());
        assertFalse(taskRoleResource.getCancelOwn());
        assertFalse(taskRoleResource.getClaim());
        assertFalse(taskRoleResource.getUnclaim());
        assertFalse(taskRoleResource.getAssign());
        assertFalse(taskRoleResource.getUnassign());
        assertFalse(taskRoleResource.getUnclaimAssign());
        assertFalse(taskRoleResource.getUnassignClaim());
        assertFalse(taskRoleResource.getUnassignAssign());
    }

    @Test
    void given_task_is_created_when_search_by_role_signature_then_task_id_is_returned() {
        transactionHelper.doInNewTransaction(() -> {
            task.setIndexed(true);
            taskResourceRepository.save(task);
        });

        String[] filterSignature = {"*:IA:*:*:1:765324"};
        //String[] roleSignature = {"IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*"};

        List<String> taskIds = taskResourceRepository.searchTasksIds(filterSignature);
        assertEquals(taskId, taskIds.get(0));
    }

    @Test
    void given_task_is_created_when_count_by_role_signature_then_correct_count_returned() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2);
        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        transactionHelper.doInNewTransaction(() -> {
            task.setIndexed(true);
            createdTask.setIndexed(true);
            taskResourceRepository.save(task);
            taskResourceRepository.save(createdTask);
        });

        String[] filterSignature = {"*:IA:*:*:1:765324"};
        //String[] roleSignature = {"IA:*:*:tribunal-caseofficer:*:r:U:*", "IA:*:*:case-manager:*:r:U:*"};

        Long taskCount = taskResourceRepository.searchTasksCount(filterSignature);
        assertEquals(2, taskCount);
    }

    @Test
    void given_multiple_tasks_created_when_find_by_task_ids_then_return_ordered_task_list() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2);
        createdTask.setCaseName("TestCaseB");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        Sort sort = Sort.by(Sort.Order.asc("caseName"));
        List<TaskResource> tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId), sort);
        assertEquals(1, tasksResult.size());

        sort = Sort.by(Sort.Order.asc("caseName"));
        tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2), sort);
        assertEquals(2, tasksResult.size());
        assertEquals(taskId, tasksResult.get(0).getTaskId());
        assertEquals(taskId2, tasksResult.get(1).getTaskId());
        assertEquals("TestCaseA", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(1).getCaseName());

        sort = Sort.by(Sort.Order.desc("caseName"));
        tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2), sort);
        assertEquals(2, tasksResult.size());
        assertEquals(taskId2, tasksResult.get(0).getTaskId());
        assertEquals(taskId, tasksResult.get(1).getTaskId());
        assertEquals("TestCaseB", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseA", tasksResult.get(1).getCaseName());
    }

    @Test
    void given_multiple_tasks_created_when_find_task_with_multiple_order_filed_then_return_ordered_task_list() {
        String taskId2 = UUID.randomUUID().toString();
        TaskResource createdTask = createTask(taskId2);
        createdTask.setCaseName("TestCaseB");
        createdTask.setCaseCategory("caseCategoryB");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask));

        String taskId3 = UUID.randomUUID().toString();
        TaskResource createdTask2 = createTask(taskId3);
        createdTask2.setCaseName("TestCaseB");
        createdTask2.setCaseCategory("caseCategoryC");

        transactionHelper.doInNewTransaction(() -> taskResourceRepository.save(createdTask2));

        Sort sort = Sort.by(Sort.Order.asc("caseName"),
            Sort.Order.desc("caseCategory"));
        List<TaskResource> tasksResult = taskResourceRepository.findAllByTaskIdIn(List.of(taskId, taskId2, taskId3),
            sort);

        assertEquals(3, tasksResult.size());
        assertEquals(taskId, tasksResult.get(0).getTaskId());
        assertEquals(taskId3, tasksResult.get(1).getTaskId());
        assertEquals(taskId2, tasksResult.get(2).getTaskId());
        assertEquals("TestCaseA", tasksResult.get(0).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(1).getCaseName());
        assertEquals("TestCaseB", tasksResult.get(2).getCaseName());
        assertEquals("caseCategoryA", tasksResult.get(0).getCaseCategory());
        assertEquals("caseCategoryC", tasksResult.get(1).getCaseCategory());
        assertEquals("caseCategoryB", tasksResult.get(2).getCaseCategory());
    }

    private void checkTaskWasSaved(String taskId) {
        assertTrue(taskResourceRepository.getByTaskId(taskId).isPresent());
    }

    private TaskResource createTask(String taskId) {
        List<NoteResource> notes = singletonList(
            new NoteResource(
                "someCode",
                "noteTypeVal",
                "userVal",
                "someContent"
            ));
        return new TaskResource(
            taskId,
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.ASSIGNED,
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
            "TestCaseA",
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
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                taskId,
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
            )),
            "caseCategoryA",
            ADDITIONAL_PROPERTIES,
                "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

}
