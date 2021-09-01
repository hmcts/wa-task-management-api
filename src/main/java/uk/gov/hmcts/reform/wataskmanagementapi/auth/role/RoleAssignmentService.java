package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class RoleAssignmentService {

    private final AuthTokenGenerator serviceAuthTokenGenerator;

    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Autowired
    public RoleAssignmentService(RoleAssignmentServiceApi roleAssignmentServiceApi,
                                 AuthTokenGenerator serviceAuthTokenGenerator) {
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
    }

    public List<RoleAssignment> getRolesForUser(String idamUserId, String authToken) {
        requireNonNull(idamUserId, "IdamUserId cannot be null");

        RoleAssignmentResource roleAssignmentResource = getRoles(idamUserId, authToken);
        return roleAssignmentResource.getRoleAssignmentResponse();
    }

    private RoleAssignmentResource getRoles(String idamUserId, String authToken) {
        try {
            return roleAssignmentServiceApi.getRolesForUser(
                idamUserId,
                authToken,
                serviceAuthTokenGenerator.generate()
            );
        } catch (FeignException ex) {
            throw new UnAuthorizedException(
                "User did not have sufficient permissions to perform this action", ex);
        }
    }

}
