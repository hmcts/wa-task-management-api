package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ExecuteTaskReconfigurationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.MarkTaskReconfigurationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.UpdateSearchIndexService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TaskOperationServiceTest {


    @Mock
    MarkTaskReconfigurationService markTaskReconfigurationService;
    @Mock
    ExecuteTaskReconfigurationService executeTaskReconfigurationService;
    @Mock
    UpdateSearchIndexService updateSearchIndexService;
    @Mock
    TaskManagementService taskManagementService;

    TaskOperationService taskOperationService;
    List<TaskResource> tasks;

    @BeforeEach
    public void setUp() {
        taskOperationService = new TaskOperationService(taskManagementService,
            List.of(markTaskReconfigurationService, executeTaskReconfigurationService, updateSearchIndexService));

        tasks = taskResources(false);

        lenient().when(markTaskReconfigurationService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.MARK_TO_RECONFIGURE))))
            .thenReturn(tasks);

        lenient().when(executeTaskReconfigurationService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.EXECUTE_RECONFIGURE))))
            .thenReturn(tasks);

        lenient().when(updateSearchIndexService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.UPDATE_SEARCH_INDEX))))
            .thenReturn(tasks);
        lenient().doNothing().when(taskManagementService).updateTaskIndex(anyString());
    }

    @Test
    void should_update_task_index_true_when_execute_reconfiguration() {
        List<TaskResource> updated = taskOperationService.performOperation(
            taskOperationRequest(TaskOperationType.EXECUTE_RECONFIGURE));

        assertNotNull(updated);
        tasks.forEach(t -> verify(taskManagementService, times(1)).updateTaskIndex(t.getTaskId()));
    }

    @Test
    void should_update_task_index_false_when_mark_reconfiguration() {
        List<TaskResource> updated = taskOperationService.performOperation(
            taskOperationRequest(TaskOperationType.MARK_TO_RECONFIGURE));

        assertNotNull(updated);
        verify(taskManagementService, never()).updateTaskIndex(anyString());
    }

    @Test
    void should_update_task_index_false_when_update_search_index() {
        List<TaskResource> updated = taskOperationService.performOperation(
            taskOperationRequest(TaskOperationType.UPDATE_SEARCH_INDEX));

        assertNotNull(updated);
        verify(taskManagementService, never()).updateTaskIndex(anyString());
    }

    private List<TaskResource> taskResources(boolean index) {
        TaskResource taskResource1 = new TaskResource(
            "1234",
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNASSIGNED,
            "someCaseId"
        );
        TaskResource taskResource2 = new TaskResource(
            "4567",
            "someTaskName",
            "someTaskType",
            CFTTaskState.ASSIGNED,
            "someCaseId"
        );
        taskResource1.setReconfigureRequestTime(OffsetDateTime.now());
        taskResource1.setIndexed(index);
        taskResource2.setReconfigureRequestTime(OffsetDateTime.now());
        taskResource2.setIndexed(index);
        return List.of(taskResource1, taskResource2);
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType type) {
        TaskOperation operation = TaskOperation.builder()
            .type(type)
            .runId("run_id1")
            .maxTimeLimit(2)
            .retryWindowHours(120)
            .build();
        return new TaskOperationRequest(operation, taskFilters());
    }

    private List<TaskFilter<?>> taskFilters() {
        TaskFilter<List<String>> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }
}
