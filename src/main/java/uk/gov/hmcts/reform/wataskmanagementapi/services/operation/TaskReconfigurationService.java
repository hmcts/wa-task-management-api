package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskExecuteReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_RECONFIGURATION_EXECUTE_TASKS_TO_RECONFIGURE_FAILED;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskReconfigurationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler;

    public TaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                      TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.taskReconfigurationTransactionHandler = taskReconfigurationTransactionHandler;
    }

    @Transactional(noRollbackFor = TaskExecuteReconfigurationException.class)
    @Async
    public CompletableFuture<TaskOperationResponse>
        performTaskReconfiguration(TaskOperationRequest taskOperationRequest) {
        return CompletableFuture.completedFuture(executeTasksToReconfigure(taskOperationRequest));
    }

    private TaskOperationResponse executeTasksToReconfigure(TaskOperationRequest taskOperationRequest) {
        log.debug("execute tasks toReconfigure request: {}", taskOperationRequest);
        OffsetDateTime reconfigureDateTime = getReconfigureRequestTime(taskOperationRequest.getTaskFilter());
        Objects.requireNonNull(reconfigureDateTime);
        List<String> taskIds = cftTaskDatabaseService
            .getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), reconfigureDateTime);
        List<TaskResource> successfulTaskResources = new ArrayList<>();

        List<String> failedTaskIds = executeReconfiguration(taskIds,
                                                            successfulTaskResources,
                                                            taskOperationRequest.getOperation().getMaxTimeLimit());

        if (!failedTaskIds.isEmpty()) {
            executeReconfiguration(
                failedTaskIds,
                successfulTaskResources,
                taskOperationRequest.getOperation().getMaxTimeLimit()
            );
            taskOperationRequest.getOperation().getMaxTimeLimit();
        }

        if (!failedTaskIds.isEmpty()) {
            configurationFailLog(failedTaskIds, taskOperationRequest.getOperation().getRetryWindowHours());
        }

        return new TaskOperationResponse(Map.of("successfulTaskResources", successfulTaskResources.size()));
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

        final OffsetDateTime endTimer = OffsetDateTime.now().plusSeconds(maxTimeLimit);
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
            taskIds.forEach(taskId -> {
                try {
                    log.info("Re-configure task-id {}", taskId);
                    // Use TaskReconfigurationTransactionHandler to reconfigure the task resource within a
                    // new transaction. This ensures that any exceptions will trigger a rollback of the transaction.
                    Optional<TaskResource> taskResource =
                        taskReconfigurationTransactionHandler.reconfigureTaskResource(taskId);
                    taskResource.ifPresent(successfulTaskResources::add);
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
