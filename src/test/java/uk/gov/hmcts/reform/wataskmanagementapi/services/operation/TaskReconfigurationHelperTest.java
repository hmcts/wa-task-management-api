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
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;

import javax.persistence.OptimisticLockException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class TaskReconfigurationHelperTest {

    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @InjectMocks TaskReconfigurationHelper taskReconfigurationHelper;

    @Test
    void should_skip_reconfigure_if_task_is_locked() {

        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.of(taskResources.get(1)));

        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(1));


        taskReconfigurationHelper.reconfigureTaskResource(taskResources.get(0).getTaskId());
        verify(configureTaskService, times(0)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());

        taskReconfigurationHelper.reconfigureTaskResource(taskResources.get(1).getTaskId());
        verify(configureTaskService, times(1)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());

    }


    @Test
    void should_retry_reconfigure_if_task_is_locked() {

        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.of(taskResources.get(1)));

        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(1));


        taskReconfigurationHelper.reconfigureTaskResource(taskResources.get(0).getTaskId());
        verify(configureTaskService, times(0)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());

        taskReconfigurationHelper.reconfigureTaskResource(taskResources.get(1).getTaskId());
        verify(configureTaskService, times(1)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());
    }
//
//    @Test
//    void should_set_indexed_true() {
//
//        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
//        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
//        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
//        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());
//
//        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
//            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());
//        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
//            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
//            .thenReturn(Optional.of(taskResources.get(0)));
//        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
//            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
//            .thenReturn(Optional.of(taskResources.get(1)));
//
//        when(configureTaskService.reconfigureCFTTask(any()))
//            .thenReturn(taskResources.get(0))
//            .thenReturn(taskResources.get(1));
//        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
//            .thenReturn(taskResources.get(0))
//            .thenReturn(taskResources.get(1));
//        when(cftTaskDatabaseService.saveTask(any()))
//            .thenReturn(taskResources.get(0))
//            .thenReturn(taskResources.get(1));
//
//        TaskOperationRequest request = new TaskOperationRequest(
//            TaskOperation.builder()
//                .type(TaskOperationType.EXECUTE_RECONFIGURE)
//                .runId("")
//                .retryWindowHours(1L)
//                .maxTimeLimit(30)
//                .build(), taskFilters
//        );
//
//        assertEquals(false,taskResources.get(0).getIndexed());
//        assertEquals(false,taskResources.get(1).getIndexed());
//
//        executeTaskReconfigurationService.performOperation(request);
//
//        assertEquals(CFTTaskState.UNASSIGNED, taskResources.get(0).getState());
//        assertEquals(CFTTaskState.ASSIGNED, taskResources.get(1).getState());
//        assertEquals(true,taskResources.get(0).getIndexed());
//        assertEquals(true,taskResources.get(1).getIndexed());
//
//    }
//
//    @Test
//    void should_retry_and_fail_reconfigure_if_task_is_locked() {
//
//        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
//        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
//        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
//        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());
//
//        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
//            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());
//        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
//            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
//            .thenThrow(new OptimisticLockException("locked"));
//        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
//            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
//            .thenThrow(new OptimisticLockException("locked"));
//
//        TaskOperationRequest request = new TaskOperationRequest(
//            TaskOperation.builder()
//                .type(TaskOperationType.EXECUTE_RECONFIGURE)
//                .runId("")
//                .retryWindowHours(1L)
//                .maxTimeLimit(30)
//                .build(), taskFilters
//        );
//
//        executeTaskReconfigurationService.performOperation(request);
//
//        verify(cftTaskDatabaseService, times(8))
//            .findByIdAndStateInObtainPessimisticWriteLock(any(), anyList());
//        verifyNoInteractions(configureTaskService);
//        verifyNoInteractions(taskAutoAssignmentService);
//    }
//
//    @Test
//    void should_not_reconfigure_for_max_time_limit() {
//
//        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
//        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
//
//        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
//            anyList(), any())).thenReturn(taskResources.stream().map(TaskResource::getTaskId).toList());
//
//        OffsetDateTime todayTestDatetime = OffsetDateTime.now();
//
//        TaskOperationRequest request = new TaskOperationRequest(
//            TaskOperation.builder()
//                .type(TaskOperationType.EXECUTE_RECONFIGURE)
//                .runId("")
//                .maxTimeLimit(2)
//                .build(), taskFilters
//        );
//
//        Map<String, Object> responseMap = executeTaskReconfigurationService.performOperation(request).getResponseMap();
//        int tasks = (int) responseMap.get("successfulTaskResources");
//        assertEquals(0, tasks);
//
//        verify(configureTaskService, times(0)).configureCFTTask(any(), any());
//        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());
//    }

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

