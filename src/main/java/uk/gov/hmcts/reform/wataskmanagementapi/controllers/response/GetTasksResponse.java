package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;

import java.util.List;

@EqualsAndHashCode
@ToString
@Getter
public class GetTasksResponse<T extends Task> {

    private final List<T> tasks;

    private final long totalRecords;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("has_more_records")
    @Schema(
        description = "Whether matching tasks exist beyond the reported total_records cap",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private final Boolean hasMoreRecords;

    public GetTasksResponse(List<T> tasks, long totalRecords) {
        this(tasks, totalRecords, null);
    }

    public GetTasksResponse(List<T> tasks, long totalRecords, Boolean hasMoreRecords) {
        this.tasks = tasks;
        this.totalRecords = totalRecords;
        this.hasMoreRecords = hasMoreRecords;
    }
}
