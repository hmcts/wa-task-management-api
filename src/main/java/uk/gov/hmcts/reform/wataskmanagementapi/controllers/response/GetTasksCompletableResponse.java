package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetTasksCompletableResponse<T extends Task> {

    private final boolean taskRequiredForEvent;
    private final List<T> tasks;

    public GetTasksCompletableResponse(boolean taskRequiredForEvent, List<T> tasks) {
        this.taskRequiredForEvent = taskRequiredForEvent;
        this.tasks = tasks;
    }

    public boolean isTaskRequiredForEvent() {
        return taskRequiredForEvent;
    }

    public List<T> getTasks() {
        return tasks;
    }
}
