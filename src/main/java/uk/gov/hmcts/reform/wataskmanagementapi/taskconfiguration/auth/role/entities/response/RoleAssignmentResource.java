package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.RoleAssignment;

import java.util.List;

@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class RoleAssignmentResource {
    private final List<RoleAssignment> roleAssignmentResponse;
    private final Object links;

    public RoleAssignmentResource(@JsonProperty("roleAssignmentResponse") List<RoleAssignment> roleAssignmentResponse,
                                  Object links) {
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
