package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

class CFTTaskDatabaseServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;
    @Autowired
    CFTTaskMapper cftTaskMapper;

    CFTTaskDatabaseService cftTaskDatabaseService;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);
    }

    @Test
    void should_succeed_and_save_task() {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        TaskResource updatedTaskResource = cftTaskDatabaseService.saveTask(taskResource);
        assertNotNull(updatedTaskResource);
        assertEquals(taskId, updatedTaskResource.getTaskId());
        assertEquals("someTaskName", updatedTaskResource.getTaskName());
        assertEquals("someTaskType", updatedTaskResource.getTaskType());
        assertEquals(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"), updatedTaskResource.getPriorityDate());
        assertEquals(UNCONFIGURED, updatedTaskResource.getState());
    }

    @Test
    void should_succeed_and_find_a_task_by_id() {

        TaskResource taskResource = createAndSaveTask(UNCONFIGURED);

        Optional<TaskResource> updatedTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get().getTaskId());
        assertEquals(taskResource.getTaskName(), updatedTaskResource.get().getTaskName());
        assertEquals(taskResource.getTaskType(), updatedTaskResource.get().getTaskType());
        assertEquals(UNCONFIGURED, updatedTaskResource.get().getState());
    }

    @Test
    void should_succeed_and_find_a_task_by_id_with_no_lock() {

        TaskResource taskResource = createAndSaveTask(UNCONFIGURED);

        Optional<TaskResource> updatedTaskResource =
            cftTaskDatabaseService.findByIdOnly(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get().getTaskId());
        assertEquals(taskResource.getTaskName(), updatedTaskResource.get().getTaskName());
        assertEquals(taskResource.getTaskType(), updatedTaskResource.get().getTaskType());
        assertEquals(UNCONFIGURED, updatedTaskResource.get().getState());
    }

    @Test
    void should_succeed_and_find_a_tasks_to_update_index() {

        createAndSaveTask(ASSIGNED);
        createAndSaveTask(UNASSIGNED);
        createAndSaveTask(COMPLETED);
        createAndSaveTask(CANCELLED);

        List<TaskResource> taskResourceToIndex = cftTaskDatabaseService.findTaskToUpdateIndex();

        assertNotNull(taskResourceToIndex);
        assertFalse(taskResourceToIndex.isEmpty());
        assertEquals(2, taskResourceToIndex.size());
        assertThat(taskResourceToIndex.stream().map(TaskResource::getState).toList(),
            containsInAnyOrder(ASSIGNED, UNASSIGNED));
    }

    private TaskResource createAndSaveTask(CFTTaskState state) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            state,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        return taskResourceRepository.save(taskResource);
    }

}
