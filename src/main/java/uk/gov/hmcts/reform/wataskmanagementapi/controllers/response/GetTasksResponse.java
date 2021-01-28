package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetTasksResponse<T extends Task> {

    private final List<T> tasks;

    public GetTasksResponse(List<T> tasks) {
        this.tasks = tasks;
    }

    public List<T> getTasks() {
        return tasks;
    }
}
