package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;

import java.util.List;

@Schema(
    name = "TaskOperationRequest",
    description = "Allows specifying certain operations on a task"
)
@EqualsAndHashCode
@ToString
public class TaskOperationRequest {

    @Schema(required = true)
    private final TaskOperation operation;
    @Schema(required = true)
    private final List<TaskFilter> taskFilter;

    @JsonCreator
    public TaskOperationRequest(TaskOperation operation,
                                @JsonProperty("taskFilter") @JsonAlias("task_filter") List<TaskFilter> taskFilter) {
        this.operation = operation;
        this.taskFilter = taskFilter;
    }

    public TaskOperation getOperation() {
        return operation;
    }

    public List<TaskFilter> getTaskFilter() {
        return taskFilter;
    }
}
