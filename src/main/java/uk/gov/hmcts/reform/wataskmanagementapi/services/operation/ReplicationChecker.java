package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
@Component
@Profile("replica | preview")
public class ReplicationChecker implements TaskOperationPerformService {
    private static final String TASK_REPLICATION_ERROR = "TASK_REPLICATION_ERROR: Task replication not found for [{}]";
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final MIReportingService miReportingService;

    public ReplicationChecker(CFTTaskDatabaseService cftTaskDatabaseService, MIReportingService miReportingService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.miReportingService = miReportingService;
    }

    @Override
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType()
            .equals(TaskOperationType.PERFORM_REPLICATION_CHECK)) {
            return performReplicationCheck();
        }
        return new TaskOperationResponse();
    }

    public TaskOperationResponse performReplicationCheck() {
        List<TaskResource> lastUpdatedTasks = cftTaskDatabaseService.findLastFiveUpdatedTasks();
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        List<TaskResource> notReplicated = new ArrayList<>();
        List<Future<?>> checks = new ArrayList<>();

        for (TaskResource task : lastUpdatedTasks) {
            Future<?> future = executor.submit(() -> {
                try {
                    await()
                        .pollInterval(1, SECONDS)
                        .atMost(20, SECONDS)
                        .until(checkTaskHistory(task));
                } catch (ConditionTimeoutException ex) {
                    replicationLog(task);
                    notReplicated.add(task);
                }
            });
            checks.add(future);
        }

        waitForCheckCompletion(checks);


        return new TaskOperationResponse(Map.of(
            "replicationCheckedTaskIds",
            lastUpdatedTasks.stream().map(TaskResource::getTaskId).collect(Collectors.toList()),
            "notReplicatedTaskIds",
            notReplicated.stream().map(TaskResource::getTaskId).collect(Collectors.toList())));
    }

    private void waitForCheckCompletion(List<Future<?>> checks) {
        while (!checks.isEmpty()) {
            Optional<Future<?>> future = checks.stream().findAny();
            if (future.isPresent() && future.get().isDone()) {
                checks.remove(future.get());
            }
        }
    }

    private void replicationLog(TaskResource notReplicated) {
        log.warn(TASK_REPLICATION_ERROR,
            "taskId: " + notReplicated.getTaskId()
            + ", lastUpdatedTimestamp: " + notReplicated.getLastUpdatedTimestamp()
            + ", lastUpdatedAction: " + notReplicated.getLastUpdatedAction()
            + ", lastUpdatedUser: " + notReplicated.getLastUpdatedUser());
    }

    private Callable<Boolean> checkTaskHistory(TaskResource task) {
        return () -> {
            List<TaskHistoryResource> taskHistoryItems =
                miReportingService.findByTaskIdOrderByLatestUpdate(task.getTaskId());
            return taskHistoryItems.stream().anyMatch(h -> h.getUpdated().equals(task.getLastUpdatedTimestamp())
                                                       && h.getUpdateAction().equals(task.getLastUpdatedAction())
                                                       && h.getUpdatedBy().equals(task.getLastUpdatedUser()));
        };
    }
}
