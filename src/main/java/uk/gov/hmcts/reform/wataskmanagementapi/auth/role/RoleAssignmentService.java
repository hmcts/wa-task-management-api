package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

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

    public List<Assignment> getRolesForUser(String idamUserId, HttpHeaders headers) {
        requireNonNull(idamUserId, "IdamUserId cannot be null");

        RoleAssignmentResponse roleAssignmentResponse = getRoles(idamUserId, headers);

        return roleAssignmentResponse.getRoleAssignments();
    }

    private RoleAssignmentResponse getRoles(String idamUserId, HttpHeaders headers) {
        try {
            return roleAssignmentServiceApi.getRolesForUser(
                idamUserId,
                headers.getFirst(AUTHORIZATION),
                serviceAuthTokenGenerator.generate()
            );
        } catch (FeignException ex) {
            throw new InsufficientPermissionsException("User did not have sufficient permissions to access task");
        }
    }

}
