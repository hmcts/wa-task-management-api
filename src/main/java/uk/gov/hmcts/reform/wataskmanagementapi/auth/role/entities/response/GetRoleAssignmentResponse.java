package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;

import java.util.List;


@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetRoleAssignmentResponse {

    private List<Assignment> roleAssignmentResponse;
    private Object links;

    private GetRoleAssignmentResponse() {
        //Hidden constructor
    }

    public GetRoleAssignmentResponse(List<Assignment> roleAssignmentResponse, Object links) {
        this.roleAssignmentResponse = roleAssignmentResponse;
        this.links = links;
    }

    public List<Assignment> getRoleAssignmentResponse() {
        return roleAssignmentResponse;
    }

}
