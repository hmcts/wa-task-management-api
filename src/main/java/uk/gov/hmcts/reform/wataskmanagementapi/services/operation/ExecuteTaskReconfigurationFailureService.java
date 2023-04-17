package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ExecuteTaskReconfigurationFailureService implements TaskOperationPerformService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public ExecuteTaskReconfigurationFailureService(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    @Override
    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType()
            .equals(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES)) {
            return executeReconfigurationFailLog(taskOperationRequest.getOperation().getRetryWindowHours());
        }
        return List.of();
    }

    private List<TaskResource> executeReconfigurationFailLog(long retryWindowHours) {
        OffsetDateTime retryWindow = OffsetDateTime.now().minusHours(retryWindowHours);

        return cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), retryWindow);
    }
}
