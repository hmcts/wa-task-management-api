package uk.gov.hmcts.reform.wataskmanagementapi.services;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public final class SearchFilterSignatureBuilder {
    private static final Set<String> WILDCARD = Collections.singleton("*");

    private SearchFilterSignatureBuilder() {
        //Utility class constructor
    }

    public static Set<String> buildFilterSignatures(SearchRequest searchTaskRequest) {
        Set<String> filterSignatures = new HashSet<>();
        for (String state : defaultToWildcard(abbreviateStates(searchTaskRequest.getCftTaskStates()))) {
            for (String jurisdiction : defaultToWildcard(searchTaskRequest.getJurisdictions())) {
                for (String roleCategory :
                    defaultToWildcard(abbreviateRoleCategories(searchTaskRequest.getRoleCategories()))) {
                    for (String workType : defaultToWildcard(searchTaskRequest.getWorkTypes())) {
                        for (String region : defaultToWildcard(searchTaskRequest.getRegion())) {
                            for (String location : defaultToWildcard(searchTaskRequest.getLocations())) {
                                filterSignatures.add(String.join(":",
                                    state, jurisdiction, roleCategory, workType, region, location));
                            }
                        }
                    }
                }
            }
        }
        return filterSignatures;
    }

    private static Collection<String> defaultToWildcard(Collection<String> strings) {
        return strings == null || strings.isEmpty() ? WILDCARD : strings;
    }

    private static Set<String> abbreviateStates(List<CFTTaskState> states) {
        return Stream.ofNullable(states)
            .flatMap(Collection::stream)
            .map(CFTTaskState::getAbbreviation)
            .collect(Collectors.toSet());
    }

    private static Set<String> abbreviateRoleCategories(List<RoleCategory> roleCategories) {
        return Stream.ofNullable(roleCategories)
            .flatMap(Collection::stream)
            .map(RoleCategory::getAbbreviation)
            .collect(Collectors.toSet());
    }
}
