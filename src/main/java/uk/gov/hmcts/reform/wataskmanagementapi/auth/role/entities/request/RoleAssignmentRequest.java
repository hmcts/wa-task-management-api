package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.util.List;

@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentRequest {

    private RoleRequest roleRequest;
    private List<RoleAssignment> requestedRoles;


    private RoleAssignmentRequest() {
        //Hidden constructor
    }

    public RoleAssignmentRequest(RoleRequest roleRequest, List<RoleAssignment> requestedRoles) {
        this.roleRequest = roleRequest;
        this.requestedRoles = requestedRoles;
    }

    public RoleRequest getRoleRequest() {
        return roleRequest;
    }

    public List<RoleAssignment> getRequestedRoles() {
        return requestedRoles;
    }

}
