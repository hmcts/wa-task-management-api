package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;

import java.time.OffsetDateTime;

@Schema(
    name = "TaskFilter",
    description = "Name of filter and value"
)
@EqualsAndHashCode
@ToString
public class ExecuteReconfigureTaskFilter implements TaskFilter<OffsetDateTime> {

    @Schema(required = true)
    private final String key;

    @Schema(required = true)
    private final OffsetDateTime values;

    @Schema(required = true)
    private final TaskFilterOperator operator;

    @JsonCreator
    public ExecuteReconfigureTaskFilter(String key, OffsetDateTime values, TaskFilterOperator operator) {
        this.key = key;
        this.values = values;
        this.operator = operator;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public OffsetDateTime getValues() {
        return values;
    }

    @Override
    public TaskFilterOperator getOperator() {
        return operator;
    }
}
