package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void should_find_by_case_id() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.getByCaseId("12345")).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService.findByCaseIdOnly("12345");

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_case_ids_states_reconfiguration_time_is_null() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByCaseIdInAndStateInAndReconfigureRequestTimeIsNull(
            List.of("1234"), List.of(CFTTaskState.ASSIGNED))).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(List.of("1234"), List.of(CFTTaskState.ASSIGNED));

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_not_null() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.findByStateInAndReconfigureRequestTimeIsNotNull(
            List.of(CFTTaskState.ASSIGNED))).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsNotNull(List.of(CFTTaskState.ASSIGNED));

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_find_by_state_and_reconfigure_request_time_is_less_than_retry() {
        TaskResource someTaskResource = mock(TaskResource.class);
        OffsetDateTime retry = OffsetDateTime.now().minusHours(2);

        when(taskResourceRepository.findByTaskIdInAndStateInAndReconfigureRequestTimeIsLessThan(
            List.of("199"), List.of(CFTTaskState.ASSIGNED), retry)).thenReturn(List.of(someTaskResource));

        final List<TaskResource> actualTaskResource = cftTaskDatabaseService
            .getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
                List.of("199"), List.of(CFTTaskState.ASSIGNED), retry);

        assertNotNull(actualTaskResource);
        assertEquals(someTaskResource, actualTaskResource.get(0));
    }

    @Test
    void should_save_task() {
        TaskResource someTaskResource = mock(TaskResource.class);

        when(taskResourceRepository.save(someTaskResource)).thenReturn(someTaskResource);

        final TaskResource actualTaskResource = cftTaskDatabaseService.saveTask(someTaskResource);

        assertNotNull(actualTaskResource);
        verify(someTaskResource, times(1)).getPriorityDate();
        verify(someTaskResource, times(1)).setPriorityDate(any());
        verify(someTaskResource, times(1)).getDueDateTime();
    }

    @Test
    void should_insert_and_lock_task() throws SQLException {

        OffsetDateTime dueDate = OffsetDateTime.now();
        OffsetDateTime created = OffsetDateTime.now().plusMinutes(1);

        lenient().doNothing().when(taskResourceRepository).insertAndLock(taskId, dueDate, created, dueDate);

        cftTaskDatabaseService.insertAndLock(taskId, dueDate);

        verify(taskResourceRepository, times(1))
            .insertAndLock(anyString(),
                any(),
                any(),
                any()
            );
    }

    @Test
    void should_return_task_resource_by_specification() {
        TaskResource taskResource = spy(TaskResource.class);
        Mockito.when(taskResourceRepository.findOne(any())).thenReturn(Optional.of(taskResource));

        final Optional<TaskResource> taskBySpecification = cftTaskDatabaseService.findTaskBySpecification(any());

        assertNotNull(taskBySpecification);
        verify(taskResourceRepository, times(1)).findOne(any());
    }

    @Test
    void should_return_empty_task_resource_by_specification() {
        Mockito.when(taskResourceRepository.findOne(any())).thenReturn(Optional.empty());

        final Optional<TaskResource> taskBySpecification = cftTaskDatabaseService.findTaskBySpecification(any());

        assertTrue(taskBySpecification.isEmpty());
        verify(taskResourceRepository, times(1)).findOne(any());
    }
}
