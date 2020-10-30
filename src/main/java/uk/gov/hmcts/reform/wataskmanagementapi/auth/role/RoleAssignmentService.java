package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignments;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class RoleAssignmentService {

    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Autowired
    public RoleAssignmentService(RoleAssignmentServiceApi roleAssignmentServiceApi) {
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
    }

    public RoleAssignments getRolesForUser(String idamUserId, HttpHeaders headers) {
        requireNonNull(idamUserId, "IdamUserId cannot be null");

        RoleAssignmentResponse roleAssignmentResponse = roleAssignmentServiceApi.getRolesForUser(
            idamUserId,
            headers.getFirst(AUTHORIZATION),
            headers.getFirst(SERVICE_AUTHORIZATION)
        );

        return classifyRolesByRoleType(roleAssignmentResponse.getRoleAssignments());
    }

    private RoleAssignments classifyRolesByRoleType(List<Assignment> assignments) {

        Set<String> roles = new HashSet<>();
        Set<Assignment> organisationRoles = new HashSet<>();
        Set<Assignment> caseRoles = new HashSet<>();

        assignments.forEach(assignment -> {
            roles.add(assignment.getRoleName());
            if (RoleType.ORGANISATION == assignment.getRoleType()) {
                organisationRoles.add(assignment);
            } else {
                caseRoles.add(assignment);
            }
        });

        return new RoleAssignments(
            roles,
            organisationRoles,
            caseRoles
        );
    }
}
