package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        TaskResource taskResource = createAndSaveTask();

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

        TaskResource taskResource = createAndSaveTask();

        Optional<TaskResource> updatedTaskResource =
            cftTaskDatabaseService.findByIdOnly(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get().getTaskId());
        assertEquals(taskResource.getTaskName(), updatedTaskResource.get().getTaskName());
        assertEquals(taskResource.getTaskType(), updatedTaskResource.get().getTaskType());
        assertEquals(UNCONFIGURED, updatedTaskResource.get().getState());
    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        return taskResourceRepository.save(taskResource);
    }

}
