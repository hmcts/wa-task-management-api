package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

import java.util.List;


@EqualsAndHashCode
@ToString
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

}
