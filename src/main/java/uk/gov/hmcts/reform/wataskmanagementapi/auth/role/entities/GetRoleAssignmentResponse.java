package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRoleAssignmentResponse {

    private List<Assignment> roleAssignmentResponse;

    private GetRoleAssignmentResponse() {
        //Hidden constructor
    }

    public GetRoleAssignmentResponse(List<Assignment> roleAssignmentResponse) {
        this.roleAssignmentResponse = roleAssignmentResponse;
    }

    public List<Assignment> getRoleAssignmentResponse() {
        return roleAssignmentResponse;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        GetRoleAssignmentResponse that = (GetRoleAssignmentResponse) object;
        return Objects.equal(roleAssignmentResponse, that.roleAssignmentResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roleAssignmentResponse);
    }

    @Override
    public String toString() {
        return "RoleAssignmentResponse{"
               + "roleAssignments=" + roleAssignmentResponse
               + '}';
    }
}
