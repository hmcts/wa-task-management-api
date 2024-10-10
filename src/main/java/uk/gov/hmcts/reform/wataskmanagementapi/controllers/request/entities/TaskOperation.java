package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;

@Schema(
    name = "TaskOperation",
    description = "Allows specifying certain operations on a task"
)
@EqualsAndHashCode
@ToString
@Builder
public class TaskOperation {

    @Schema(requiredMode = REQUIRED)
    @JsonProperty("name")
    private TaskOperationType type;

    @JsonProperty("run_id")
    private String runId;

    @JsonProperty("max_time_limit")
    private long maxTimeLimit;
    @JsonProperty("retry_window_hours")
    private long retryWindowHours;

    public TaskOperationType getType() {
        return type;
    }

    public String getRunId() {
        return runId;
    }

    public long getMaxTimeLimit() {
        return maxTimeLimit;
    }

    public long getRetryWindowHours() {
        return retryWindowHours;
    }


}
