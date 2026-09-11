package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.signature.SearchFilterSignatureBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_TASK_SEARCH_GIN_INDEX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.MANAGE_PERMISSION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.OWN_AND_CLAIM_PERMISSION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.signature.RoleSignatureBuilder.READ_PERMISSION;

@Slf4j
@Service
public class CFTTaskSearchService {

    private static final int ROLE_ASSIGNMENTS_LOG_THRESHOLD = 100;
    private static final String SERVICE_USER_ID = "wa-task-management-api";
    private static final String SERVICE_EMAIL = "wa-task-management-api@hmcts.net";

    private final TaskResourceRepository tasksRepository;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final int countLimit;

    @Autowired
    public CFTTaskSearchService(TaskResourceRepository tasksRepository,
                                LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                @Value("${config.search.countLimit}") int countLimit) {
        this.tasksRepository = tasksRepository;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.countLimit = countLimit;
    }

    public SearchResult searchForTaskIds(int firstResult,
                                         int maxResults,
                                         SearchRequest searchRequest,
                                         List<RoleAssignment> roleAssignments) {

        if (ROLE_ASSIGNMENTS_LOG_THRESHOLD <= roleAssignments.size()) {
            log.info("Total volume of Role Assignments for current user: {}", roleAssignments.size());
        }

        List<String> excludeCaseIds = buildExcludedCaseIds(roleAssignments);

        if (isSearchIndexSearchEnabled()) {
            log.info("Search using legacy search_index");
            return searchUsingSearchIndex(firstResult, maxResults, searchRequest, roleAssignments, excludeCaseIds);
        }
        log.info("Search using new index");
        return searchUsingRoleCriteria(firstResult, maxResults, searchRequest, roleAssignments, excludeCaseIds);
    }

    public SearchResult searchUsingSearchIndex(int firstResult,
                                                int maxResults,
                                                SearchRequest searchRequest,
                                                List<RoleAssignment> roleAssignments,
                                                List<String> excludeCaseIds) {

        Set<String> filterSignature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);
        Set<String> roleSignature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        log.info("Task search for filter signatures {} \nrole signatures {} \nexcluded case ids {}",
                 filterSignature, roleSignature, excludeCaseIds
        );
        List<String> taskIds = tasksRepository.searchTasksIdsUsingSearchIndex(
            firstResult, maxResults, filterSignature, roleSignature, excludeCaseIds, searchRequest
        );

        if (isEmpty(taskIds)) {
            return new SearchResult(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCountUsingSearchIndex(
            filterSignature, roleSignature, excludeCaseIds, searchRequest);
        return new SearchResult(taskIds, count);
    }


    public SearchResult searchUsingRoleCriteria(int firstResult,
                                                 int maxResults,
                                                 SearchRequest searchRequest,
                                                 List<RoleAssignment> roleAssignments,
                                                 List<String> excludeCaseIds) {

        List<TaskSearchRoleCriteria> roleCriteria = buildRoleCriteria(roleAssignments, searchRequest);

        log.info("Task search for roleCriteria  {} \nexcluded case ids {}", roleCriteria, excludeCaseIds);

        List<String> taskIds = tasksRepository.searchTasksIdsUsingRoleCriteria(
            firstResult, maxResults, roleCriteria, excludeCaseIds, searchRequest
        );

        if (isEmpty(taskIds)) {
            return new SearchResult(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCountUsingRoleCriteria(
            roleCriteria, excludeCaseIds, searchRequest, countLimit
        );
        return new SearchResult(taskIds, count);
    }

    private List<String> buildExcludedCaseIds(List<RoleAssignment> roleAssignments) {
        return roleAssignments.stream()
            .filter(ra -> ra.getGrantType() == GrantType.EXCLUDED)
            .map(ra -> ra.getAttributes().get(RoleAttributeDefinition.CASE_ID.value()))
            .filter(Objects::nonNull)
            .toList();
    }

    private List<TaskSearchRoleCriteria> buildRoleCriteria(List<RoleAssignment> roleAssignments,
                                                           SearchRequest searchRequest) {
        List<TaskSearchRoleCriteria> roleCriteria = new ArrayList<>();

        for (RoleAssignment roleAssignment : roleAssignments) {
            if (!canMatchSearch(roleAssignment, searchRequest)) {
                continue;
            }

            for (String authorizationValue : authorizations(roleAssignment, searchRequest)) {
                roleCriteria.add(new TaskSearchRoleCriteria(
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.JURISDICTION).orElse(null),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.REGION).orElse(null),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.BASE_LOCATION).orElse(null),
                    roleAssignment.getRoleName(),
                    roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).orElse(null),
                    permissionRequirement(searchRequest),
                    roleAssignment.getClassification().getAbbreviation(),
                    authorizationValue
                ));
            }
        }

        return roleCriteria;
    }

    private boolean canMatchSearch(RoleAssignment roleAssignment, SearchRequest searchRequest) {
        return roleAssignment.getRoleName() != null
               && roleAssignment.getClassification() != null
               && roleAssignment.getClassification().getAbbreviation() != null
               && List.of(GrantType.STANDARD, GrantType.SPECIFIC, GrantType.CHALLENGED).contains(
                   roleAssignment.getGrantType())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.JURISDICTION,
                                       searchRequest.getJurisdictions())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.REGION, searchRequest.getRegions())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.BASE_LOCATION,
                                       searchRequest.getLocations())
               && matchesRoleAttribute(roleAssignment, RoleAttributeDefinition.CASE_ID, searchRequest.getCaseIds());
    }

    private boolean matchesRoleAttribute(RoleAssignment roleAssignment,
                                         RoleAttributeDefinition attribute,
                                         List<String> requestedValues) {
        var attributeValue = roleAssignment.getAttributeValue(attribute);
        return isEmpty(requestedValues)
               || attributeValue.isEmpty()
               || requestedValues.contains(attributeValue.orElse(null));
    }

    private List<String> authorizations(RoleAssignment roleAssignment, SearchRequest searchRequest) {
        List<String> authorizationValues = new ArrayList<>();
        authorizationValues.add(null);
        var caseId = roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID);

        if (searchRequest.isAvailableTasksOnly()
            && caseId.isEmpty()
            && !isEmpty(roleAssignment.getAuthorisations())) {
            authorizationValues.addAll(roleAssignment.getAuthorisations());
        }

        return authorizationValues.stream().distinct().toList();
    }

    private String permissionRequirement(SearchRequest searchRequest) {
        if (searchRequest.isAvailableTasksOnly()) {
            return OWN_AND_CLAIM_PERMISSION;
        } else if (searchRequest.isAllWork()) {
            return MANAGE_PERMISSION;
        }
        return READ_PERMISSION;
    }

    private boolean isSearchIndexSearchEnabled() {
        return launchDarklyFeatureFlagProvider != null
            && launchDarklyFeatureFlagProvider.getBooleanValue(
            WA_TASK_SEARCH_GIN_INDEX,
            SERVICE_USER_ID,
            SERVICE_EMAIL
        );
    }
}
