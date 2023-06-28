package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;

public interface TaskOperationPerformService {

    TaskOperationResponse performOperation(TaskOperationRequest request);
}
