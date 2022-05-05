package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;

@Schema(
    name = "TaskAttribute",
    description = "Name of attribute and value"
)
@EqualsAndHashCode
@ToString
public class TaskAttribute {

    private final TaskAttributeDefinition name;
    private final Object value;

    @JsonCreator
    public TaskAttribute(TaskAttributeDefinition name,
                         Object value) {
        this.name = name;
        this.value = value;
    }

    public TaskAttributeDefinition getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
