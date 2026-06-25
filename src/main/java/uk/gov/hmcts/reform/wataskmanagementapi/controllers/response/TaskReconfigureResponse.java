package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.ArrayList;
import java.util.List;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TaskReconfigureResponse {

    private List<TaskResource> tasks = new ArrayList<>();

    public TaskReconfigureResponse addTasksItem(TaskResource task) {
        tasks.add(task);
        return this;
    }
}
