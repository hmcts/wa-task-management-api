package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;

@Slf4j
@Component
public class ExecuteTaskReconfigurationService implements TaskOperationPerformService {

    private final TaskReconfigurationService taskReconfigurationService;

    public ExecuteTaskReconfigurationService(TaskReconfigurationService taskReconfigurationService) {
        this.taskReconfigurationService = taskReconfigurationService;
    }

    @Override
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType().equals(TaskOperationType.EXECUTE_RECONFIGURE)) {
            taskReconfigurationService.performTaskReconfiguration(taskOperationRequest);
            return new TaskOperationResponse();
        }
        return new TaskOperationResponse();
    }



}
