package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CFTTaskDatabaseServiceTest extends CamundaHelpers {

    @Mock
    private TaskResourceRepository taskResourceRepository;

    private CFTTaskDatabaseService cftTaskDatabaseService;

    private String taskId;
    private TaskResource taskResource;

    @BeforeEach
    void setUp() {

        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);
        taskId = UUID.randomUUID().toString();

    }

    @Test
    void findByIdAndObtainPessimisticWriteLock_test() {

        Optional<TaskResource> optionalTaskResource = Optional.of(createTestTask());

        when(taskResourceRepository.findById(taskId))
            .thenReturn(optionalTaskResource);

        Optional<TaskResource> actualTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());

    }

    @Test
    void findByIdOnly_test() {

        Optional<TaskResource> optionalTaskResource = Optional.of(createTestTask());

        when(taskResourceRepository.getByTaskId(taskId))
            .thenReturn(optionalTaskResource);

        Optional<TaskResource> actualTaskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());

    }

    @Test
    void saveTask_test() {

        taskResource = createTestTask();

        when(taskResourceRepository.save(taskResource))
            .thenReturn(taskResource);

        TaskResource actualTaskResource = cftTaskDatabaseService.saveTask(taskResource);

        assertNotNull(actualTaskResource);

    }

    @Test
    void insertAndLock_test() throws SQLException {

        taskResource = createTestTask();

        cftTaskDatabaseService.insertAndLock(taskId);

        verify(taskResourceRepository, times(1)).insertAndLock(taskId);

    }


    private TaskResource createTestTask() {

        return new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
            "someCaseId"
        );

    }

}
