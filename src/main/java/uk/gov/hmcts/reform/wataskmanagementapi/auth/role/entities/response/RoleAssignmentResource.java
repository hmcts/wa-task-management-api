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
    private List<RoleAssignment> roleAssignmentResponse;

    public RoleAssignmentResource() {
        //Default constructor
    }

    public RoleAssignmentResource(List<RoleAssignment> roleAssignmentResponse) {
        this.roleAssignmentResponse = roleAssignmentResponse;
    }

    public List<RoleAssignment> getRoleAssignmentResponse() {
        return roleAssignmentResponse;
    }

}
