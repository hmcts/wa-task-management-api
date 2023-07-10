package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.EXECUTE_RECONFIGURE_FAILURES;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ExecuteTaskReconfigurationFailureService implements TaskOperationPerformService {

    private static final String ERROR_TITLE = "Task Execute Reconfiguration Failed for following tasks {}";

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public ExecuteTaskReconfigurationFailureService(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    @Override
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType()
            .equals(EXECUTE_RECONFIGURE_FAILURES)) {
            return executeReconfigurationFailLog(taskOperationRequest.getOperation().getRetryWindowHours());
        }
        return new TaskOperationResponse();
    }

    private TaskOperationResponse executeReconfigurationFailLog(long retryWindowHours) {
        OffsetDateTime retryWindow = OffsetDateTime.now().minusHours(retryWindowHours);

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), retryWindow);

        if (!taskResources.isEmpty()) {
            log.warn(ERROR_TITLE,
                taskResources.stream()
                    .map(task -> "\n" + task.getTaskId()
                                 + ", " + task.getTaskName()
                                 + ", " + task.getState()
                                 + ", " + task.getReconfigureRequestTime()
                                 + ", " + task.getLastReconfigurationTime())
                    .collect(Collectors.joining()));
        }
        return new TaskOperationResponse(Map.of("successfulTaskResources", taskResources.size()));
    }
}
