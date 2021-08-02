package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.CftRepositoryBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CFTTaskDatabaseServiceTest extends CftRepositoryBaseTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    CFTTaskDatabaseService cftTaskDatabaseService;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);

    }

    @Test
    void should_succeed_and_save_task() {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType"
        );

        TaskResource updatedTaskResource = cftTaskDatabaseService.saveTask(taskResource);
        assertNotNull(updatedTaskResource);
        assertEquals(updatedTaskResource.getTaskId(), taskId);
        assertEquals(updatedTaskResource.getTaskName(), "someTaskName");
        assertEquals(updatedTaskResource.getTaskType(), "someTaskType");
    }

    @Test
    void should_succeed_and_find_a_task_by_id() {

        TaskResource taskResource = createAndSaveTask();

        Optional<TaskResource> updatedTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(updatedTaskResource.get().getTaskId(), taskResource.getTaskId());
        assertEquals(updatedTaskResource.get().getTaskName(), taskResource.getTaskName());
        assertEquals(updatedTaskResource.get().getTaskType(), taskResource.getTaskType());
    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType"
        );

        return cftTaskDatabaseService.saveTask(taskResource);
    }


}
