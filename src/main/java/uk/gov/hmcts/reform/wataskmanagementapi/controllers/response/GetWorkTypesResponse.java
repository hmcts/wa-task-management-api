package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.List;

@EqualsAndHashCode
@ToString
public class GetWorkTypesResponse {

    private final List<WorkType> workTypes;

    public GetWorkTypesResponse(List<WorkType> workTypes) {
        this.workTypes = workTypes;
    }

    public List<WorkType> getWorkTypes() {
        return workTypes;
    }
}
