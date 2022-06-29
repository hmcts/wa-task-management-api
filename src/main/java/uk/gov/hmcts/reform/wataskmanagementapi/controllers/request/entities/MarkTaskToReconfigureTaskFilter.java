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
public class MarkTaskToReconfigureTaskFilter implements TaskFilter<List<String>> {

    @Schema(required = true)
    private final String key;

    @Schema(required = true)
    private final List<String> values;

    @Schema(required = true)
    private final TaskFilterOperator operator;

    @JsonCreator
    public MarkTaskToReconfigureTaskFilter(String key, List<String> values, TaskFilterOperator operator) {
        this.key = key;
        this.values = values;
        this.operator = operator;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public TaskFilterOperator getOperator() {
        return operator;
    }
}
