package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.OptimisticLockException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ExecuteTaskReconfigurationServiceTest {
    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;


    @Mock
    TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler;
    @InjectMocks
    private ExecuteTaskReconfigurationService executeTaskReconfigurationService;


    @Test
    void should_get_tasks_with_reconfigure_request_time_and_set_to_null() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());
        when(taskReconfigurationTransactionHandler.reconfigureTaskResource(anyString()))
            .thenReturn(Optional.of(taskResources.get(0)))
            .thenReturn(Optional.of(taskResources.get(1)));

        OffsetDateTime todayTestDatetime = OffsetDateTime.now();

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .maxTimeLimit(30)
                .runId("")
                .build(), taskFilters
        );

        Map<String, Object> responseMap = executeTaskReconfigurationService.performOperation(request).getResponseMap();
        int tasks = (int) responseMap.get("successfulTaskResources");
        assertEquals(2, tasks);

        verify(taskReconfigurationTransactionHandler, times(2)).reconfigureTaskResource(any());

    }

    @Test
    void should_not_execute_reconfigure_if_tasks_are_not_found() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(List.of());

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .maxTimeLimit(2)
                .build(), taskFilters
        );

        Map<String, Object> responseMap = executeTaskReconfigurationService.performOperation(request).getResponseMap();
        int tasks = (int) responseMap.get("successfulTaskResources");
        assertEquals(0, tasks);

        verify(taskReconfigurationTransactionHandler, times(0)).reconfigureTaskResource(any());
    }

    @Test
    void should_skip_reconfigure_if_task_state_is_not_unassigned_or_assigned(CapturedOutput output) {
        TaskResource taskResource3 = new TaskResource(
            "12345",
            "someTaskName",
            "someTaskType",
            CFTTaskState.COMPLETED,
            "someCaseId"
        );
        TaskResource taskResource4 = new TaskResource(
            "12346",
            "someTaskName",
            "someTaskType",
            CFTTaskState.TERMINATED,
            "someCaseId"
        );
        TaskResource taskResource5 = new TaskResource(
            "12347",
            "someTaskName",
            "someTaskType",
            CFTTaskState.ASSIGNED,
            "someCaseId"
        );

        List<TaskResource> taskResources =
            List.of(taskResourcesToReconfigure(OffsetDateTime.now()).get(0),
                    taskResourcesToReconfigure(OffsetDateTime.now()).get(1),
                    taskResource3, taskResource4, taskResource5);

        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(2).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(3).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(4).setReconfigureRequestTime(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(anyList(), any()))
            .thenReturn(taskResources.stream().filter(taskResource -> (
                taskResource.getState() == CFTTaskState.UNASSIGNED || taskResource.getState() == CFTTaskState.ASSIGNED))
                            .map(TaskResource::getTaskId).toList());

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .build(), taskFilters
            );
        executeTaskReconfigurationService.performOperation(request);

        verify(taskReconfigurationTransactionHandler, times(3)).reconfigureTaskResource(any());
    }

    @Test
    void should_retry_reconfigure_if_task_is_not_reconfigured_and_exception_is_thrown() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());
        when(taskReconfigurationTransactionHandler.reconfigureTaskResource(taskResources.get(0).getTaskId()))
            .thenThrow(new OptimisticLockException("locked")).thenReturn(Optional.of(taskResources.get(0)));
        when(taskReconfigurationTransactionHandler.reconfigureTaskResource(taskResources.get(1).getTaskId()))
            .thenReturn(Optional.of(taskResources.get(1)));

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .build(), taskFilters
        );

        executeTaskReconfigurationService.performOperation(request);
        // Attempt to reconfigure both tasks initially, calling the method twice.
        // If an OptimisticLockException is thrown for the first task, it will be retried,
        // resulting in a total of three method calls.
        verify(taskReconfigurationTransactionHandler, times(3)).reconfigureTaskResource(any());
    }

    @Test
    void should_not_reconfigure_for_max_time_limit() {

        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .maxTimeLimit(0)
                .build(), taskFilters
        );

        Map<String, Object> responseMap = executeTaskReconfigurationService.performOperation(request).getResponseMap();
        int tasks = (int) responseMap.get("successfulTaskResources");
        assertEquals(0, tasks);

        verify(taskReconfigurationTransactionHandler, times(0)).reconfigureTaskResource(any());
    }

    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.now().minus(Duration.ofDays(10)), TaskFilterOperator.AFTER);
        return List.of(filter);
    }

    private List<TaskResource> taskResourcesToReconfigure(OffsetDateTime reconfigureTime) {
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
        if (Objects.nonNull(reconfigureTime)) {
            taskResource1.setLastReconfigurationTime(reconfigureTime.minusDays(2));
            taskResource2.setLastReconfigurationTime(reconfigureTime.minusDays(2));
        }
        return List.of(taskResource1, taskResource2);
    }

}

