package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.List;

public interface TaskOperationService {

    List<TaskResource> performOperation(TaskOperationRequest request);
}
