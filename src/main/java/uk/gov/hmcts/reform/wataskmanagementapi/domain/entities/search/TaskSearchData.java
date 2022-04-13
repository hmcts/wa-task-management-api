package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;

import java.util.List;

public class TaskSearchData implements SearchData {

    private final SearchPermissionsRequired searchPermissionsRequired;

    private final List<RoleAssignmentForSearch> roleAssignmentForSearches;

    private final SearchTaskRequest requestData;

    public TaskSearchData(SearchPermissionsRequired searchPermissionsRequired,
                          List<RoleAssignmentForSearch> roleAssignmentForSearches,
                          SearchTaskRequest requestData) {
        this.searchPermissionsRequired = searchPermissionsRequired;
        this.roleAssignmentForSearches = roleAssignmentForSearches;
        this.requestData = requestData;
    }

    @Override
    public SearchPermissionsRequired getPermissionsRequired() {
        return null;
    }

    @Override
    public List<RoleAssignmentForSearch> getRoleAssignmentData() {
        return null;
    }

    @Override
    public SearchTaskRequest getRequestData() {
        return null;
    }
}
