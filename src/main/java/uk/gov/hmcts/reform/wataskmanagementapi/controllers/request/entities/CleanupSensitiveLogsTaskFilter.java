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
public class CleanupSensitiveLogsTaskFilter implements TaskFilter<OffsetDateTime> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final String key;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final OffsetDateTime values;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final TaskFilterOperator operator;

    @JsonCreator
    public CleanupSensitiveLogsTaskFilter(String key, OffsetDateTime values, TaskFilterOperator operator) {
        this.key = key;
        this.values = values;
        this.operator = operator;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public TaskFilterOperator getOperator() {
        return operator;
    }

    @Override
    public OffsetDateTime getValues() {
        return values;
    }
}
