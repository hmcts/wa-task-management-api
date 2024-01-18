package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import java.util.List;

@EqualsAndHashCode
@ToString
public class SearchTasksResponse {

    private final List<Task> tasks;

    public SearchTasksResponse(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
