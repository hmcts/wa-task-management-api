package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(anyString()))
            .thenReturn(Optional.of(taskResources.get(0)))
            .thenReturn(Optional.of(taskResources.get(1)));
        when(configureTaskService.configureCFTTask(any(), any()))
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
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "", 2, 120), taskFilters
        );

        List<TaskResource> taskResourcesReconfigured = executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(2)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(2)).reAutoAssignCFTTask(any());

        taskResourcesReconfigured.forEach(taskResource -> {
            assertNull(taskResource.getReconfigureRequestTime());
            assertTrue(taskResource.getLastReconfigurationTime().isAfter(todayTestDatetime));
        });

    }

    @Test
    void should_not_execute_reconfigure_if_tasks_are_not_found() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(anyString()))
            .thenReturn(Optional.empty());

        TaskOperationRequest request = new TaskOperationRequest(
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "", 2, 120), taskFilters
        );

        List<TaskResource> taskResourcesReconfigured = executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(0)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());

        assertEquals(0, taskResourcesReconfigured.size());
    }

    @Test
    void should_skip_reconfigure_if_task_is_locked() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskResources.get(0).getTaskId()))
            .thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskResources.get(1).getTaskId()))
            .thenReturn(Optional.of(taskResources.get(1)));

        when(configureTaskService.configureCFTTask(any(), any()))
            .thenReturn(taskResources.get(1));
        when(taskAutoAssignmentService.reAutoAssignCFTTask(any()))
            .thenReturn(taskResources.get(1));
        when(cftTaskDatabaseService.saveTask(any()))
            .thenReturn(taskResources.get(1));

        TaskOperationRequest request = new TaskOperationRequest(
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "",2, 120), taskFilters
        );

        List<TaskResource> taskResourcesReconfigured = executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(1)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(1)).reAutoAssignCFTTask(any());

        assertEquals(1, taskResourcesReconfigured.size());
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

    @Test
    void should_get_reconfiguration_fail_log() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(anyString()))
            .thenReturn(null);

        when(cftTaskDatabaseService
                 .getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(anyList(), anyList(), any())
            ).thenReturn(taskResources);

        TaskOperationRequest request = new TaskOperationRequest(
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "", 2, 120), taskFilters
        );

        assertThatThrownBy(() -> executeTaskReconfigurationService.performOperation(request))
            .hasNoCause()
            .hasMessageContaining("Task Execute Reconfiguration Failed: "
                            + "Task Reconfiguration process failed to execute reconfiguration for the following tasks:")
            .hasMessageContaining("1234 ,someTaskName ,UNASSIGNED",  OffsetDateTime.now(), "null")
            .hasMessageContaining("4567 ,someTaskName ,ASSIGNED",  OffsetDateTime.now(), "null");
    }

    @Test
    void should_get_no_tasks_on_reconfiguration_fail_log() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(anyString()))
            .thenReturn(null);

        when(cftTaskDatabaseService
                 .getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(anyList(), anyList(), any())
        ).thenReturn(Collections.emptyList());

        TaskOperationRequest request = new TaskOperationRequest(
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "", 2, 120), taskFilters
        );

        List<TaskResource> taskResourcesReconfigured = executeTaskReconfigurationService.performOperation(request);
        assertEquals(0, taskResourcesReconfigured.size());
    }

    @Test
    void should_not_reconfigure_for_max_time_limit() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService.getActiveTasksAndReconfigureRequestTimeIsNotNull(
            anyList())).thenReturn(taskResources);

        OffsetDateTime todayTestDatetime = OffsetDateTime.now();

        TaskOperationRequest request = new TaskOperationRequest(
            new TaskOperation(TaskOperationName.EXECUTE_RECONFIGURE, "", 2, 0), taskFilters
        );

        List<TaskResource> taskResourcesReconfigured = executeTaskReconfigurationService.performOperation(request);

        verify(configureTaskService, times(0)).configureCFTTask(any(), any());
        verify(taskAutoAssignmentService, times(0)).reAutoAssignCFTTask(any());

        taskResourcesReconfigured.forEach(taskResource -> {
            assertNull(taskResource.getReconfigureRequestTime());
            assertTrue(taskResource.getLastReconfigurationTime().isAfter(todayTestDatetime));
        });

    }

}

