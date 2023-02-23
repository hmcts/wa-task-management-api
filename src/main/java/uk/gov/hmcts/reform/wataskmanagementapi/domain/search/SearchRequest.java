package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

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
    private List<String> region;
    private List<String> caseIds;
    private List<String> users;
    private List<String> taskIds;
    private List<String> taskTypes;
    private List<String> workTypes;
    private List<RoleCategory> roleCategories;
    private RequestContext requestContext;
    private boolean availableTasksOnly;
    private List<SortingParameter> sortingParameters;
}
