package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

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

    @JsonCreator
    public TaskOperation(TaskOperationName name, @JsonProperty("runId") @JsonAlias("run_id") String runId) {
        this.name = name;
        this.runId = runId;
    }

    public TaskOperationName getName() {
        return name;
    }

    public String getRunId() {
        return runId;
    }
}
