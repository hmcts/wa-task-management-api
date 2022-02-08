package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.QueryRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PRIVATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.RESTRICTED;

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.AvoidDuplicateLiterals"})
public class TaskConfigurationRoleAssignmentService {

    private final AuthTokenGenerator serviceAuthTokenGenerator;

    private final RoleAssignmentServiceApi roleAssignmentServiceApi;

    private final IdamTokenGenerator systemUserIdamToken;

    @Autowired
    public TaskConfigurationRoleAssignmentService(RoleAssignmentServiceApi roleAssignmentServiceApi,
                                                  AuthTokenGenerator serviceAuthTokenGenerator,
                                                  IdamTokenGenerator systemUserIdamToken) {
        this.roleAssignmentServiceApi = roleAssignmentServiceApi;
        this.serviceAuthTokenGenerator = serviceAuthTokenGenerator;
        this.systemUserIdamToken = systemUserIdamToken;
    }

    public List<RoleAssignment> queryRolesForAutoAssignmentByCaseId(TaskResource taskResource) {
        requireNonNull(taskResource, "taskResource cannot be null");
        RoleAssignmentResource roleAssignmentResponse = performSearch(buildQueryForAutoAssignment(taskResource));
        log.debug(
            "Roles successfully retrieved from RoleAssignment Service for taskId '{}' and caseId '{}'",
            taskResource.getTaskId(),
            taskResource.getCaseId()
        );

        return roleAssignmentResponse.getRoleAssignmentResponse();
    }

    public List<RoleAssignment> searchRolesByCaseId(String caseId) {
        requireNonNull(caseId, "caseId cannot be null");

        RoleAssignmentResource roleAssignmentResponse = performSearch(buildQueryRequest(caseId));
        log.debug("Roles successfully retrieved from RoleAssignment Service for caseId '{}'", caseId);

        return roleAssignmentResponse.getRoleAssignmentResponse();
    }

    public List<RoleAssignment> getRolesByUserId(String userId) {
        requireNonNull(userId, "userId cannot be null");

        RoleAssignmentResource roleAssignmentResponse = roleAssignmentServiceApi.getRolesForUser(
            userId,
            systemUserIdamToken.generate(),
            serviceAuthTokenGenerator.generate()
        );

        return roleAssignmentResponse.getRoleAssignmentResponse();
    }

    public RoleAssignmentResource performSearch(MultipleQueryRequest multipleQueryRequest) {
        try {
            return roleAssignmentServiceApi.queryRoleAssignments(
                systemUserIdamToken.generate(),
                serviceAuthTokenGenerator.generate(),
                multipleQueryRequest
            );
        } catch (FeignException ex) {
            throw new ServerErrorException(
                "Could not retrieve role assignments when performing the search", ex);
        }
    }

    private MultipleQueryRequest buildQueryForAutoAssignment(TaskResource taskResource) {
        Set<TaskRoleResource> rolesInTask = taskResource.getTaskRoleResources();

        String caseId = taskResource.getCaseId();
        List<Classification> securityClassifications =
            evaluateEqualOrHigherClassification(taskResource.getSecurityClassification());

        Set<String> roleNamesFound = rolesInTask.stream()
            .map(role -> role.getOwn() && role.getAutoAssignable() ? role.getRoleName() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        QueryRequest queryRequest = QueryRequest.builder()
            .roleName(new ArrayList<>(roleNamesFound))
            .classification(securityClassifications)
            .grantType(singletonList(GrantType.SPECIFIC))
            .validAt(LocalDateTime.now())
            .hasAttributes(singletonList("caseId"))
            .attributes(Map.of("caseId", List.of(caseId)))
            .build();

        return MultipleQueryRequest.builder().queryRequests(singletonList(queryRequest)).build();
    }

    private List<Classification> evaluateEqualOrHigherClassification(SecurityClassification securityClassification) {
        switch (securityClassification) {
            case PUBLIC:
                return asList(PUBLIC, PRIVATE, RESTRICTED);
            case PRIVATE:
                return asList(PRIVATE, RESTRICTED);
            case RESTRICTED:
                return singletonList(RESTRICTED);
            default:
                throw new IllegalStateException("Unexpected classification value " + securityClassification);
        }
    }

    private MultipleQueryRequest buildQueryRequest(String caseId) {
        QueryRequest queryRequest = QueryRequest.builder()
            .roleType(singletonList(RoleType.CASE))
            .roleName(singletonList("tribunal-caseworker"))
            .validAt(LocalDateTime.now())
            .hasAttributes(singletonList("caseId"))
            .attributes(Map.of("caseId", List.of(caseId)))
            .build();

        return MultipleQueryRequest.builder().queryRequests(singletonList(queryRequest)).build();
    }

}
