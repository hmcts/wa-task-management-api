package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskExecuteReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_RECONFIGURATION_EXECUTE_TASKS_TO_RECONFIGURE_FAILED;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ExecuteTaskReconfigurationService implements TaskOperationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;

    public ExecuteTaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                             ConfigureTaskService configureTaskService,
                                             TaskAutoAssignmentService taskAutoAssignmentService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
    }

    @Override
    @Transactional(noRollbackFor = TaskExecuteReconfigurationException.class)
    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getName().equals(TaskOperationName.EXECUTE_RECONFIGURE)) {
            return executeTasksToReconfigure(taskOperationRequest);
        }
        return List.of();
    }

    private List<TaskResource> executeTasksToReconfigure(TaskOperationRequest request) {
        OffsetDateTime reconfigureDateTime = getReconfigureRequestTime(request.getTaskFilter());
        Objects.requireNonNull(reconfigureDateTime);

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsNotNull(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        List<TaskResource> successfulTaskResources = new ArrayList<>();

        List<String> taskIds = taskResources.stream()
            .map(TaskResource::getTaskId)
            .collect(Collectors.toList());

        List<String> failedTaskIds = executeReconfiguration(taskIds,
                                                            successfulTaskResources,
                                                            request.getOperation().getMaxTimeLimit());

        if (!failedTaskIds.isEmpty()) {
            failedTaskIds = executeReconfiguration(failedTaskIds,
                                                   successfulTaskResources,
                                                   request.getOperation().getMaxTimeLimit());
        }

        if (!failedTaskIds.isEmpty()) {
            configurationFailLog(failedTaskIds, request.getOperation().getRetryWindowHours());
        }

        return successfulTaskResources;
    }

    private void configurationFailLog(List<String> failedTaskIds, long retryWindowHours) {
        OffsetDateTime retryWindow = OffsetDateTime.now().minusHours(retryWindowHours);

        List<TaskResource> failedTasksToReport = cftTaskDatabaseService
            .getTasksByTaskIdAndStateInAndReconfigureRequestTimeIsLessThanRetry(
                failedTaskIds, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), retryWindow);

        if (!failedTasksToReport.isEmpty()) {
            throw new TaskExecuteReconfigurationException(TASK_RECONFIGURATION_EXECUTE_TASKS_TO_RECONFIGURE_FAILED,
                                                          failedTasksToReport
            );
        }
    }

    private List<String> executeReconfiguration(List<String> taskIds,
                                                List<TaskResource> successfulTaskResources,
                                                long maxTimeLimit) {

        final OffsetDateTime endTimer  = OffsetDateTime.now().plusSeconds(maxTimeLimit);
        List<String> failedTaskIds = reconfigureTasks(taskIds, successfulTaskResources, endTimer);

        List<String> secondaryFailedTaskIds = new ArrayList<>();

        if (!failedTaskIds.isEmpty()) {
            secondaryFailedTaskIds = reconfigureTasks(failedTaskIds, successfulTaskResources, endTimer);
        }

        return secondaryFailedTaskIds;
    }

    private List<String> reconfigureTasks(List<String> taskIds, List<TaskResource> successfulTaskResources,
                                          OffsetDateTime endTimer) {
        List<String> failedTaskIds = new ArrayList<>();
        if (endTimer.isAfter(OffsetDateTime.now())) {
            taskIds.stream()
                .forEach(taskId -> {

                    try {
                        Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
                            .findByIdAndObtainPessimisticWriteLock(taskId);

                        if (optionalTaskResource.isPresent()) {
                            TaskResource taskResource = optionalTaskResource.get();
                            taskResource = configureTaskService.reconfigureCFTTask(taskResource);
                            taskResource = taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);
                            taskResource.setReconfigureRequestTime(null);
                            taskResource.setLastReconfigurationTime(OffsetDateTime.now());
                            successfulTaskResources.add(cftTaskDatabaseService.saveTask(taskResource));
                        }
                    } catch (Exception e) {
                        log.error("Error configuring task (id={}) ", taskId, e);
                        failedTaskIds.add(taskId);
                    }
                });
        }

        return failedTaskIds;
    }

    private OffsetDateTime getReconfigureRequestTime(List<TaskFilter<?>> taskFilters) {

        return taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("reconfigure_request_time"))
            .findFirst()
            .map(filter -> ((ExecuteReconfigureTaskFilter) filter).getValues())
            .orElseGet(() -> null);
    }

}
