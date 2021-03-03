package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        RoleAssignmentRequest that = (RoleAssignmentRequest) object;
        return Objects.equal(roleRequest, that.roleRequest)
               && Objects.equal(requestedRoles, that.requestedRoles);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roleRequest, requestedRoles);
    }


    @Override
    public String toString() {
        return "RoleAssignmentRequest{"
               + "roleRequest=" + roleRequest
               + ", requestedRoles=" + requestedRoles
               + '}';
    }
}
