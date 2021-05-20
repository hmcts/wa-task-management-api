package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetTasksCompletableResponse<T extends Task> {

    private final List<T> tasks;

    public GetTasksCompletableResponse(List<T> tasks) {
        this.tasks = tasks;
    }

    public List<T> getTasks() {
        return tasks;
    }
}
