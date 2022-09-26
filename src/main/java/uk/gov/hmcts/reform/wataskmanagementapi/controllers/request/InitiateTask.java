package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;

public interface InitiateTask<T> {
    InitiateTaskOperation getOperation();

    T getTaskAttributes();
}
