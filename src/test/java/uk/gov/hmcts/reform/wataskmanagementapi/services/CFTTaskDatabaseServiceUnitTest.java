package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CFTTaskDatabaseServiceUnitTest {

    @Mock
    private TaskResourceRepository taskRepository;

    private CFTTaskDatabaseService cftTaskDatabaseService;

    @BeforeEach
    public void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskRepository);
    }

    @Test
    void should_return_task_resource_by_specification() {
        TaskResource taskResource = spy(TaskResource.class);
        Mockito.when(taskRepository.findOne(any())).thenReturn(Optional.of(taskResource));

        final Optional<TaskResource> taskBySpecification = cftTaskDatabaseService.findTaskBySpecification(any());

        assertNotNull(taskBySpecification);
        verify(taskRepository, times(1)).findOne(any());
    }
}
