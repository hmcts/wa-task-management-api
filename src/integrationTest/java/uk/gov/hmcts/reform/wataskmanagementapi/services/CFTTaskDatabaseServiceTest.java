package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
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
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType"
        );

        taskResourceRepository.saveAndFlush(taskResource);
        Optional<TaskResource> updatedTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId);

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(updatedTaskResource.get().getTaskId(), taskId);
        assertEquals(updatedTaskResource.get().getTaskName(), "taskName");
        assertEquals(updatedTaskResource.get().getTaskType(), "startAppeal");
    }

}
