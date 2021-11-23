package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CFTTaskDatabaseServiceTest {

    @Mock
    TaskResourceRepository taskResourceRepository;

    private CFTTaskDatabaseService cftTaskDatabaseService;
    private String taskId;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);

        taskId = UUID.randomUUID().toString();
    }

    @Test
    void should_find_by_id_and_obtain_pessimistic_write_lock() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findById(taskId)).thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource =
            cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_find_by_id_only() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.getByTaskId(taskId)).thenReturn(Optional.of(someTaskResource));

        final Optional<TaskResource> actualTaskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertNotNull(actualTaskResource);
        assertTrue(actualTaskResource.isPresent());
        assertEquals(someTaskResource, actualTaskResource.get());
    }

    @Test
    void should_save_task() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.save(someTaskResource)).thenReturn(someTaskResource);

        final TaskResource actualTaskResource = cftTaskDatabaseService.saveTask(someTaskResource);

        assertNotNull(actualTaskResource);
    }
}
