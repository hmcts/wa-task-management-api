package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.MappedTask;

public class GetTaskResponse<T extends MappedTask> {

    private final T task;

    public GetTaskResponse(T task) {
        this.task = task;
    }

    public T getTask() {
        return task;
    }
}
