package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSearchIndexServiceTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @InjectMocks
    private UpdateSearchIndexService updateSearchIndexService;

    private final TaskOperationRequest request = new TaskOperationRequest(
        TaskOperation.builder()
            .type(TaskOperationType.UPDATE_SEARCH_INDEX).build(),
        List.of()
    );

    @Test
    void should_process_update_search_index_operation() {
        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");
        assertEquals(0, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
    }

    @Test
    void should_process_update_search_index_operation_for_empty_list() {
        when(cftTaskDatabaseService.findTaskToUpdateIndex()).thenReturn(List.of());

        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");

        assertEquals(0, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
    }

    @Test
    void should_process_update_search_index_operation_for_not_found_task() {
        TaskResource resource = new TaskResource("1", "newTask", "review", CFTTaskState.UNASSIGNED);
        when(cftTaskDatabaseService.findTaskToUpdateIndex()).thenReturn(List.of(resource));

        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");

        assertEquals(0, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
    }

    @Test
    void should_process_update_search_index_operation_if_task_not_found() {
        TaskResource resource = new TaskResource("1", "newTask", "review", CFTTaskState.UNASSIGNED);
        when(cftTaskDatabaseService.findTaskToUpdateIndex()).thenReturn(List.of(resource));
        when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock("1")).thenReturn(Optional.empty());

        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");

        assertEquals(0, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
    }

    @Test
    void should_process_update_search_index_operation_and_save_task() {
        TaskResource resource = new TaskResource("1", "newTask", "review", CFTTaskState.UNASSIGNED);
        when(cftTaskDatabaseService.findTaskToUpdateIndex()).thenReturn(List.of(resource));
        when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock("1"))
            .thenReturn(Optional.of(resource));
        when(cftTaskDatabaseService.saveTask(any(TaskResource.class))).thenReturn(resource);

        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");
        assertEquals(1, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
        verify(cftTaskDatabaseService, times(1)).findByIdAndWaitAndObtainPessimisticWriteLock("1");
        verify(cftTaskDatabaseService, times(1)).saveTask(any(TaskResource.class));
    }

    @Test
    void should_process_update_search_index_operation_and_save_for_multiple_tasks() {
        TaskResource resource = spy(TaskResource.class);
        when(resource.getTaskId()).thenReturn("1");
        when(cftTaskDatabaseService.findTaskToUpdateIndex()).thenReturn(List.of(resource, resource));
        lenient().when(cftTaskDatabaseService.findByIdAndWaitAndObtainPessimisticWriteLock("1"))
            .thenReturn(Optional.of(resource));
        lenient().when(cftTaskDatabaseService.saveTask(argThat(t -> t.getTaskId().equals("1")))).thenReturn(resource);

        Map<String, Object> resourceMap = updateSearchIndexService.performOperation(request).getResponseMap();
        int tasks = (int) resourceMap.get("successfulTaskResources");
        assertEquals(2, tasks);
        verify(cftTaskDatabaseService, times(1)).findTaskToUpdateIndex();
        verify(cftTaskDatabaseService, times(2)).findByIdAndWaitAndObtainPessimisticWriteLock("1");
        verify(cftTaskDatabaseService, times(2)).saveTask(any(TaskResource.class));
    }

}
