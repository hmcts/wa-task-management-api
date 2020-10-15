package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

public class GetTaskResponse<T extends CamundaTask> {

    private final T task;

    public GetTaskResponse(T task) {
        this.task = task;
    }

    public T getTask() {
        return task;
    }
}
