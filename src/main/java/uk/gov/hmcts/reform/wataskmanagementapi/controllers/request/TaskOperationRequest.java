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
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@Schema(
    name = "TaskOperationRequest",
    description = "Allows specifying certain operations on a task"
)
@EqualsAndHashCode
@ToString
public class TaskOperationRequest {

    @Schema(required = true)
    private TaskOperation operation;

    @Schema(
        required = true,
        description = "https://tools.hmcts.net/confluence/display/WA/WA+Task+Management+API+Guidelines")
    @NotEmpty(message = "At least one task_filter element is required.")
    private List<@Valid TaskFilter<?>> taskFilter;

    private TaskOperationRequest() {
        //Default constructor for deserialization
        super();
    }

    @JsonCreator
    public TaskOperationRequest(TaskOperation operation,
                                @JsonProperty("taskFilter") @JsonAlias("task_filter") List<TaskFilter<?>> taskFilter) {
        this.operation = operation;
        this.taskFilter = taskFilter;
    }

    public TaskOperationRequest(List<TaskFilter<?>> taskFilter) {
        this.taskFilter = taskFilter;
    }

    public TaskOperation getOperation() {
        return operation;
    }

    public List<TaskFilter<?>> getTaskFilter() {
        return taskFilter;
    }
}
