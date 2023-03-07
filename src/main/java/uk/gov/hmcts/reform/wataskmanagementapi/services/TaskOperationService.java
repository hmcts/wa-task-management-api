package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;

public interface TaskOperationService {

    TaskOperationResponse performOperation(TaskOperationRequest request);
}
