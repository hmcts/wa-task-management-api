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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.OptimisticLockException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ExecuteTaskReconfigurationServiceTest {

    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @InjectMocks
    private ExecuteTaskReconfigurationService executeTaskReconfigurationService;

    @Test
    void should_get_tasks_with_reconfigure_request_time_and_set_to_null() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(anyString(), anyList()))
            .thenReturn(Optional.of(taskResources.get(0)))
            .thenReturn(Optional.of(taskResources.get(1)));
        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));

        OffsetDateTime todayTestDatetime = OffsetDateTime.now();

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .maxTimeLimit(2)
                .runId("")
                .build(), taskFilters
        );

        Map<String, Object> responseMap = executeTaskReconfigurationService.performOperation(request).getResponseMap();
        int tasks = (int) responseMap.get("successfulTaskResources");
        assertEquals(2, tasks);

        verify(configureTaskService, times(2)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(2)).reAutoAssignCFTTask(any());

    }

    @Test
    void should_not_execute_reconfigure_if_tasks_are_not_found() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(anyString(), anyList()))
            .thenReturn(Optional.empty());

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

        verify(configureTaskService, times(0)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());
    }

    @Test
    void should_skip_reconfigure_if_task_is_locked() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
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

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .maxTimeLimit(2)
                .build(), taskFilters
        );

        executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(1)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());
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



        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
                anyList(), any())).thenReturn(taskResources);
        //update task resource state just before getting lock
        taskResources.get(1).setState(CFTTaskState.CANCELLED);
        cftTaskDatabaseService.deleteTasks(List.of(taskResources.get(4).getTaskId()));

        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.of(taskResources.get(0)));
        when(cftTaskDatabaseService.findByIdOnly(taskResources.get(1).getTaskId()))
            .thenReturn(Optional.of(taskResources.get(1)));
        when(cftTaskDatabaseService.findByIdOnly(taskResources.get(2).getTaskId()))
            .thenReturn(Optional.of(taskResources.get(2)));
        when(cftTaskDatabaseService.findByIdOnly(taskResources.get(3).getTaskId()))
            .thenReturn(Optional.of(taskResources.get(3)));
        when(cftTaskDatabaseService.findByIdOnly(taskResources.get(4).getTaskId()))
            .thenReturn(Optional.empty());

        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(0));

        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(0));

        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(0));

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

        verify(configureTaskService, times(1)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());
        verify(cftTaskDatabaseService, times(4)).findByIdOnly(any());

        assertAll(
            () -> assertTrue(output.getOut().contains("did not execute reconfigure for Task Resource: taskId: "
                                                          + taskResources.get(1).getTaskId())),
            () -> assertTrue(output.getOut().contains("did not execute reconfigure for Task Resource: taskId: "
                                                          + taskResources.get(2).getTaskId())),
            () -> assertTrue(output.getOut().contains("did not execute reconfigure for Task Resource: taskId: "
                                                          + taskResources.get(3).getTaskId())),
            () -> assertTrue(output.getOut().contains("Could not find task to reconfigure : taskId: "
                                                          + taskResources.get(4).getTaskId()))

        );
        assertEquals(CFTTaskState.UNASSIGNED, taskResources.get(0).getState());
        assertEquals(CFTTaskState.CANCELLED, taskResources.get(1).getState());
        assertEquals(CFTTaskState.COMPLETED, taskResources.get(2).getState());
        assertEquals(CFTTaskState.TERMINATED, taskResources.get(3).getState());
    }


    @Test
    void should_retry_and_succeed_reconfigure_if_task_is_locked() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenThrow(new OptimisticLockException("locked")).thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenThrow(new OptimisticLockException("locked")).thenReturn(Optional.of(taskResources.get(1)));

        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(1));

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .build(), taskFilters
        );

        executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(1)).reconfigureCFTTask(any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());
    }

    @Test
    void should_set_indexed_true() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.of(taskResources.get(0)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenReturn(Optional.of(taskResources.get(1)));

        when(configureTaskService.reconfigureCFTTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(0))
            .thenReturn(taskResources.get(1));

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .build(), taskFilters
        );

        assertEquals(false,taskResources.get(0).getIndexed());
        assertEquals(false,taskResources.get(1).getIndexed());

        executeTaskReconfigurationService.performOperation(request);

        assertEquals(CFTTaskState.UNASSIGNED, taskResources.get(0).getState());
        assertEquals(CFTTaskState.ASSIGNED, taskResources.get(1).getState());
        assertEquals(true,taskResources.get(0).getIndexed());
        assertEquals(true,taskResources.get(1).getIndexed());

    }

    @Test
    void should_retry_and_fail_reconfigure_if_task_is_locked() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());
        taskResources.get(0).setReconfigureRequestTime(OffsetDateTime.now());
        taskResources.get(1).setReconfigureRequestTime(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(0).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenThrow(new OptimisticLockException("locked"));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResources.get(1).getTaskId(), List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED)))
            .thenThrow(new OptimisticLockException("locked"));

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("")
                .retryWindowHours(1L)
                .maxTimeLimit(30)
                .build(), taskFilters
        );

        executeTaskReconfigurationService.performOperation(request);

        verify(cftTaskDatabaseService, times(8))
            .findByIdAndStateInObtainPessimisticWriteLock(any(), anyList());
        verifyNoInteractions(configureTaskService);
        verifyNoInteractions(taskAutoAssignmentService);
    }

    @Test
    void should_not_reconfigure_for_max_time_limit() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeGreaterThan(
            anyList(), any())).thenReturn(taskResources);

        OffsetDateTime todayTestDatetime = OffsetDateTime.now();

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

        verify(configureTaskService, times(0)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());
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

