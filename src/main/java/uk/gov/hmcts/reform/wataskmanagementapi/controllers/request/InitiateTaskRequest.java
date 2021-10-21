package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;

import java.util.List;

@ApiModel(
    value = "InitiateTaskRequest",
    description = "Allows specifying certain operations to initiate a task"
)
@EqualsAndHashCode
@ToString
public class InitiateTaskRequest {

    private final InitiateTaskOperation operation;
    private final List<TaskAttribute> taskAttributes;

    @JsonCreator
    public InitiateTaskRequest(@JsonProperty("operation") InitiateTaskOperation operation,
                               @JsonProperty("taskAttributes") List<TaskAttribute> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public InitiateTaskOperation getOperation() {
        return operation;
    }

    public List<TaskAttribute> getTaskAttributes() {
        return taskAttributes;
    }
}
