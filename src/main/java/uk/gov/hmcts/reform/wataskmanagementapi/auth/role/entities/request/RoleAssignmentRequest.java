package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

import java.util.List;

@EqualsAndHashCode
@ToString
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
