package uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.util.List;

@EqualsAndHashCode
@ToString
public class AccessControlResponse {

    private UserInfo userInfo;
    private List<RoleAssignment> roleAssignments;

    private AccessControlResponse() {
        //Hidden constructor
    }

    public AccessControlResponse(UserInfo userInfo, List<RoleAssignment> roleAssignments) {
        this.userInfo = userInfo;
        this.roleAssignments = roleAssignments;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public List<RoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }
}
