package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;

@Schema(
    name = "TaskOperation",
    description = "Allows specifying certain operations on a task"
)
@EqualsAndHashCode
@ToString
public class TaskOperation {

    @Schema(required = true)
    private final TaskOperationName name;

    private final String runId;

    private final long retryWindowHours;
    private final long maxTimeLimit;

    @JsonCreator
    public TaskOperation(TaskOperationName name,
                         @JsonProperty("runId") @JsonAlias("run_id") String runId,
                         @JsonProperty("retryWindowHours") @JsonAlias("retryWindowHours") long retryWindowHours,
                         @JsonProperty("maxTimeLimit") @JsonAlias("maxTimeLimit") long maxTimeLimit) {
        this.name = name;
        this.runId = runId;
        this.retryWindowHours = retryWindowHours;
        this.maxTimeLimit = maxTimeLimit;
    }

    public TaskOperationName getName() {
        return name;
    }

    public String getRunId() {
        return runId;
    }

    public long getRetryWindowHours() {
        return retryWindowHours;
    }

    public long getMaxTimeLimit() {
        return maxTimeLimit;
    }
}
