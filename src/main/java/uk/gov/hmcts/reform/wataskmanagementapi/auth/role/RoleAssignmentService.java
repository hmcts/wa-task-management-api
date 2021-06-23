package uk.gov.hmcts.reform.wataskmanagementapi.auth.role;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.request.QueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
@Slf4j
public class RoleAssignmentService {

    private final AuthTokenGenerator serviceAuthTokenGenerator;
    private final RoleAssignmentServiceApi roleAssignmentServiceApi;
    private final IdamTokenGenerator systemUserIdamToken;

    @Autowired
    public RoleAssignmentService(RoleAssignmentServiceApi roleAssignmentServiceApi,
                                 AuthTokenGenerator serviceAuthTokenGenerator,
                                 IdamTokenGenerator systemUserIdamToken) {
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.systemUserIdamToken = systemUserIdamToken;
    }

    public List<Assignment> searchRolesByCaseId(String caseId) {
        requireNonNull(caseId, "caseId cannot be null");

        GetRoleAssignmentResponse roleAssignmentResponse = performSearch(buildQueryRequest(caseId));
        log.debug("Roles successfully retrieved from RoleAssignment Service for caseId '{}'", caseId);

        return roleAssignmentResponse.getRoleAssignmentResponse();
    }

    public GetRoleAssignmentResponse performSearch(QueryRequest queryRequest) {
        try {
            return roleAssignmentServiceApi.queryRoleAssignments(
                systemUserIdamToken.generate(),
                serviceAuthTokenGenerator.generate(),
                queryRequest
            );
        } catch (FeignException ex) {
            throw new ServerErrorException(
                "Could not retrieve role assignments when performing the search", ex);
        }
    }

    private QueryRequest buildQueryRequest(String caseId) {
        return QueryRequest.builder()
            .roleType(singletonList(RoleType.CASE))
            .roleName(singletonList("tribunal-caseworker"))
            .validAt(LocalDateTime.now())
            .attributes(Map.of("caseId", List.of(caseId)))
            .build();
    }

    public List<Assignment> getRolesForUser(String idamUserId, String authToken) {
        requireNonNull(idamUserId, "IdamUserId cannot be null");

        GetRoleAssignmentResponse getRoleAssignmentResponse = getRoles(idamUserId, authToken);
        return getRoleAssignmentResponse.getRoleAssignmentResponse();
    }

    private GetRoleAssignmentResponse getRoles(String idamUserId, String authToken) {
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
