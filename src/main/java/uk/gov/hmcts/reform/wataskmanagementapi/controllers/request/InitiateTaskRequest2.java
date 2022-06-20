package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;

import java.util.Map;

@Schema(
    name = "InitiateTaskRequest",
    description = "Allows specifying certain operations to initiate a task"
)
@EqualsAndHashCode
@ToString
public class InitiateTaskRequest2 {

    private final InitiateTaskOperation operation;
    private final Map<String, Object> taskAttributes;

    @JsonCreator
    public InitiateTaskRequest2(InitiateTaskOperation operation,
                                Map<String, Object> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public InitiateTaskOperation getOperation() {
        return operation;
    }

    public Map<String, Object> getTaskAttributes() {
        return taskAttributes;
    }
}
