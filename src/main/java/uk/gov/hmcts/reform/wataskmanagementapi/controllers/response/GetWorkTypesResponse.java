package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetWorkTypesResponse {

    @JsonProperty("work_types")
    private final List<WorkType> workTypes;

    public GetWorkTypesResponse(List<WorkType> workTypes) {
        this.workTypes = workTypes;
    }

    public List<WorkType> getWorkTypes() {
        return workTypes;
    }
}
