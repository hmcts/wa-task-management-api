package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ApiModel(
    value = "TaskAttribute",
    description = "Name of attribute and value"
)
@EqualsAndHashCode
@ToString
public class TaskAttribute {

    private final String name;
    private final String value;

    @JsonCreator
    public TaskAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
