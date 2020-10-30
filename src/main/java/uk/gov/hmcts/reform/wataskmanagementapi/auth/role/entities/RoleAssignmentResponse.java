package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentResponse {

    private List<Assignment> roleAssignments;

    private RoleAssignmentResponse() {
        //Hidden constructor
    }

    public RoleAssignmentResponse(List<Assignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<Assignment> getRoleAssignments() {
        return roleAssignments;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        RoleAssignmentResponse that = (RoleAssignmentResponse) object;
        return Objects.equal(roleAssignments, that.roleAssignments);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roleAssignments);
    }

    @Override
    public String toString() {
        return "RoleAssignmentResponse{"
               + "roleAssignments=" + roleAssignments
               + '}';
    }
}
