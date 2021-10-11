package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetWorkTypesResponse<T extends WorkType> {

    @JsonProperty("work_types")
    private final List<T> workTypes;

    public GetWorkTypesResponse(List<T> workTypes) {
        this.workTypes = workTypes;
    }

    public List<T> getWorkTypes() {
        return workTypes;
    }
}
