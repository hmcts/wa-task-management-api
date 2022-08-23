package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;

import java.util.List;

@Schema(
    name = "InitiateTaskRequest",
    description = "Allows specifying certain operations to initiate a task"
)
@Value
public class InitiateTaskRequest implements InitiateTask<List<TaskAttribute>> {
    List<TaskAttribute> taskAttributes;
    InitiateTaskOperation operation;

    @JsonCreator
    public InitiateTaskRequest(InitiateTaskOperation operation,
                               List<TaskAttribute> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }
}
