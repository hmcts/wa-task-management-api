package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import java.util.List;

@Getter
@Setter
public class SearchTaskRequest {
    private List<CFTTaskState> cftTaskStates;
    private List<String> jurisdictions;
    private List<String> locations;
    private List<String> caseIds;
    private List<String> users;
    private List<String> taskIds;
    private List<String> taskTypes;
    private List<String> workTypes;
    private List<String> roleCategories;
    private RequestContext requestContext;
    private boolean availableTasksOnly;
    private List<SortingParameter> sortingParameters;
}
