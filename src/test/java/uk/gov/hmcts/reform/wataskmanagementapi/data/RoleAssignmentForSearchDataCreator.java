package uk.gov.hmcts.reform.wataskmanagementapi.data;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;

public class RoleAssignmentForSearchDataCreator {

    private RoleAssignmentForSearchDataCreator() {
    }

    public static RoleAssignmentForSearch fromRoleAssignment(RoleAssignment roleAssignment) {

        return new RoleAssignmentForSearch(roleAssignment);

    }
}
