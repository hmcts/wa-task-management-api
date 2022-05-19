package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;

import java.util.List;

@Schema(
    name = "TaskFilter",
    description = "Name of filter and value"
)
@EqualsAndHashCode
@ToString
public class TaskFilter {

    @Schema(required = true)
    private final String key;

    @Schema(required = true)
    private final List<Object> values;

    @Schema(required = true)
    private final TaskFilterOperator operator;

    @JsonCreator
    public TaskFilter(String key, List<Object> values, TaskFilterOperator operator) {
        this.key = key;
        this.values = values;
        this.operator = operator;
    }

    public String getKey() {
        return key;
    }

    public List<Object> getValues() {
        return values;
    }

    public TaskFilterOperator getOperator() {
        return operator;
    }
}
