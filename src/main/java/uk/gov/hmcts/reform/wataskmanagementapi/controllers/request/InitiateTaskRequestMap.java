package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;

import java.util.Map;

@Schema(
    name = "InitiateTaskRequest",
    description = "Allows specifying certain operations to initiate a task"
)
@Value
public class InitiateTaskRequestMap implements InitiateTask<Map<String, Object>> {
    @Schema(name = "task_attributes")
    Map<String, Object> taskAttributes;
    InitiateTaskOperation operation;

    @JsonCreator
    public InitiateTaskRequestMap(InitiateTaskOperation operation,
                                  Map<String, Object> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }
}
