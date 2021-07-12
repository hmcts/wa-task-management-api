package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;

import java.util.List;

@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentResource {
    private final List<RoleAssignment> roleAssignmentResponse;
    private final Object links;

    public RoleAssignmentResource(List<RoleAssignment> roleAssignmentResponse) {
        this(roleAssignmentResponse, null);
    }

    public RoleAssignmentResource(List<RoleAssignment> roleAssignmentResponse, Object links) {
        this.roleAssignmentResponse = roleAssignmentResponse;
        this.links = links;
    }

    public List<RoleAssignment> getRoleAssignmentResponse() {
        return roleAssignmentResponse;
    }

    public Object getLinks() {
        return links;
    }
}
