package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype.TaskTypeResponse;

import java.util.Set;

@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class GetTaskTypesResponse {

    @JsonProperty("task_types")
    private Set<TaskTypeResponse> taskTypeResponses;

    public GetTaskTypesResponse(Set<TaskTypeResponse> taskTypeResponses) {
        this.taskTypeResponses = taskTypeResponses;
    }

    public Set<TaskTypeResponse> getTaskTypeResponses() {
        return taskTypeResponses;
    }

}
