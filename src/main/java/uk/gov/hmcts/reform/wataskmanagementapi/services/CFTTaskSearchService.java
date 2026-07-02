package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_SEARCH_INDEX_SEARCH_ENABLED;
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

    @Autowired
    public CFTTaskSearchService(TaskResourceRepository tasksRepository,
                                LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider) {
        this.tasksRepository = tasksRepository;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
    }

    public CFTTaskSearchService(TaskResourceRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
        this.launchDarklyFeatureFlagProvider = null;
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
            return searchUsingSearchIndex(firstResult, maxResults, searchRequest, roleAssignments, excludeCaseIds);
        }

        return searchUsingRoleCriteria(firstResult, maxResults, searchRequest, roleAssignments, excludeCaseIds);
    }

    private SearchResult searchUsingSearchIndex(int firstResult,
                                                int maxResults,
                                                SearchRequest searchRequest,
                                                List<RoleAssignment> roleAssignments,
                                                List<String> excludeCaseIds) {

        if (ROLE_ASSIGNMENTS_LOG_THRESHOLD <= roleAssignments.size()) {
            log.info("Total volume of Role Assignments for current user: {}", roleAssignments.size());
        }

        Set<String> filterSignature = SearchFilterSignatureBuilder.buildFilterSignatures(searchRequest);
        Set<String> roleSignature = RoleSignatureBuilder.buildRoleSignatures(roleAssignments, searchRequest);

        log.info("Task search for filter signatures {} \nrole signatures {} \nexcluded case ids {}",
                 filterSignature, roleSignature, excludeCaseIds
        );
        List<String> taskIds = tasksRepository.searchTasksIdsOld(
            firstResult, maxResults, filterSignature, roleSignature, excludeCaseIds, searchRequest
        );

        if (isEmpty(taskIds)) {
            return new SearchResult(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCountOld(filterSignature, roleSignature, excludeCaseIds, searchRequest);
        return new SearchResult(taskIds, count);
    }

    private SearchResult searchUsingRoleCriteria(int firstResult,
                                                 int maxResults,
                                                 SearchRequest searchRequest,
                                                 List<RoleAssignment> roleAssignments,
                                                 List<String> excludeCaseIds) {

        List<TaskSearchRoleCriteria> roleCriteria = buildRoleCriteria(roleAssignments, searchRequest);

        log.info("Task search excluded case ids {}", excludeCaseIds);
        List<String> taskIds = tasksRepository.searchTasksIds(
            firstResult, maxResults, roleCriteria, excludeCaseIds, searchRequest
        );

        if (isEmpty(taskIds)) {
            return new SearchResult(List.of(), 0);
        }

        Long count = tasksRepository.searchTasksCount(roleCriteria, excludeCaseIds, searchRequest);
        return new SearchResult(taskIds, count);
    }

    private boolean isSearchIndexSearchEnabled() {
        return launchDarklyFeatureFlagProvider != null
               && launchDarklyFeatureFlagProvider.getBooleanValue(
                   WA_SEARCH_INDEX_SEARCH_ENABLED,
                   SERVICE_USER_ID,
                   SERVICE_EMAIL
               );
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
        return isEmpty(requestedValues)
               || roleAssignment.getAttributeValue(attribute).isEmpty()
               || requestedValues.contains(roleAssignment.getAttributeValue(attribute).get());
    }

    private List<String> authorizations(RoleAssignment roleAssignment, SearchRequest searchRequest) {
        List<String> authorizationValues = new ArrayList<>();
        authorizationValues.add(null);

        if (searchRequest.isAvailableTasksOnly()
            && roleAssignment.getAttributeValue(RoleAttributeDefinition.CASE_ID).isEmpty()
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

    public record SearchResult(List<String> taskIds, long totalRecords) {
    }
}
