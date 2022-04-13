package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;

import java.util.List;

/*
Each Search request should call the TaskSearchService.
The TaskSearchService requires an instance of SearchData
 */
public interface SearchData {

    SearchPermissionsRequired getPermissionsRequired();

    List<RoleAssignmentForSearch> getRoleAssignmentData();

    SearchTaskRequest getRequestData();

}
