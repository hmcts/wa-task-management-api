package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentRequest {

    private RoleRequest roleRequest;
    private List<Assignment> requestedRoles;


    private RoleAssignmentRequest() {
        //Hidden constructor
    }

    public RoleAssignmentRequest(RoleRequest roleRequest, List<Assignment> requestedRoles) {
        this.roleRequest = roleRequest;
        this.requestedRoles = requestedRoles;
    }

    public RoleRequest getRoleRequest() {
        return roleRequest;
    }

    public List<Assignment> getRequestedRoles() {
        return requestedRoles;
    }
}
