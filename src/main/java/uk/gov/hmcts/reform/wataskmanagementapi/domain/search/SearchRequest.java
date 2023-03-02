package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class SearchRequest {
    private List<CFTTaskState> cftTaskStates;
    private List<String> jurisdictions;
    private List<String> locations;
    private List<String> regions;
    private List<String> caseIds;
    private List<String> users;
    private List<String> taskTypes;
    private List<String> workTypes;
    private List<RoleCategory> roleCategories;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private RequestContext requestContext;
    private List<SortingParameter> sortingParameters;

    public boolean isAvailableTasksOnly() {
        return requestContext != null && requestContext.equals(RequestContext.AVAILABLE_TASKS);
    }

    public boolean isAllWork() {
        return requestContext != null && requestContext.equals(RequestContext.ALL_WORK);
    }
}
