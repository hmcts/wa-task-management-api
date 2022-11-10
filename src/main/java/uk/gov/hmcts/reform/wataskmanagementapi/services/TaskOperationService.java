package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;

import java.util.List;

public interface TaskOperationService {

    List<TaskResource> performOperation(TaskOperationRequest request);
}
