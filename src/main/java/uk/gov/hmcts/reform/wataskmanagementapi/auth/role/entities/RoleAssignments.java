package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignments {

    private Set<String> roles;
    private Set<Assignment> organisationalRoles;
    private Set<Assignment> caseRoles;


    private RoleAssignments() {
        //Hidden constructor
    }

    public RoleAssignments(Set<String> roles, Set<Assignment> organisationalRoles, Set<Assignment> caseRoles) {
        this.roles = roles;
        this.organisationalRoles = organisationalRoles;
        this.caseRoles = caseRoles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<Assignment> getOrganisationalRoles() {
        return organisationalRoles;
    }

    public Set<Assignment> getCaseRoles() {
        return caseRoles;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        RoleAssignments that = (RoleAssignments) object;
        return Objects.equal(roles, that.roles)
               && Objects.equal(organisationalRoles, that.organisationalRoles)
               && Objects.equal(caseRoles, that.caseRoles);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roles, organisationalRoles, caseRoles);
    }

    @Override
    public String toString() {
        return "RoleAssignments{"
               + "roles=" + roles
               + ", organisationalRoles=" + organisationalRoles
               + ", caseRoles=" + caseRoles
               + '}';
    }
}
