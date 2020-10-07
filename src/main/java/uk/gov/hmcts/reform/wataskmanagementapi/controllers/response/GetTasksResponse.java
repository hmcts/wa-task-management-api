package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

import java.util.List;

public class GetTasksResponse<T extends CamundaTask> {

    private final List<T> tasks;

    public GetTasksResponse(List<T> tasks) {
        this.tasks = tasks;
    }

    public List<T> getTasks() {
        return tasks;
    }
}
