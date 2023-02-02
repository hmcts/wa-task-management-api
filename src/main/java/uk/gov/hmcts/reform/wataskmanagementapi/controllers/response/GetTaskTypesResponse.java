package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype.TaskTypeResponse;

import java.util.List;

@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class GetTaskTypesResponse {

    @JsonProperty("task_types")
    private List<TaskTypeResponse> taskTypeResponses;

    public GetTaskTypesResponse(List<TaskTypeResponse> taskTypeResponses) {
        this.taskTypeResponses = taskTypeResponses;
    }

    public List<TaskTypeResponse> getTaskTypeResponses() {
        return taskTypeResponses;
    }

}
