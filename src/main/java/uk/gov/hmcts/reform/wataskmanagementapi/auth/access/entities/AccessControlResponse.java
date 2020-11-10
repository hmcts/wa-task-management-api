package uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;

import java.util.List;

@EqualsAndHashCode
@ToString
public class AccessControlResponse {

    private UserInfo userInfo;
    private List<Assignment> roleAssignments;

    private AccessControlResponse() {
        //Hidden constructor
    }

    public AccessControlResponse(UserInfo userInfo, List<Assignment> roleAssignments) {
        this.userInfo = userInfo;
        this.roleAssignments = roleAssignments;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public List<Assignment> getRoleAssignments() {
        return roleAssignments;
    }
}
