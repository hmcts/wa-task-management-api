package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

@EqualsAndHashCode
@ToString
public class GetTaskResponse<T extends Task> {

    private final T task;

    public GetTaskResponse(T task) {
        this.task = task;
    }

    public T getTask() {
        return task;
    }
}
