package uk.gov.hmcts.reform.wataskmanagementapi.services.signature;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public final class SearchFilterSignatureBuilder {
    private static final Set<String> WILDCARD = Collections.singleton("*");

    private SearchFilterSignatureBuilder() {
        //Utility class constructor
    }

    public static Set<String> buildFilterSignatures(SearchRequest searchTaskRequest) {
        return defaultToWildcard(CFTTaskState.getAbbreviations(searchTaskRequest.getCftTaskStates())).stream()
            .flatMap(state -> defaultToWildcard(searchTaskRequest.getJurisdictions()).stream()
                .flatMap(jurisdiction ->
                    defaultToWildcard(RoleCategory.getAbbreviations(searchTaskRequest.getRoleCategories())).stream()
                        .flatMap(roleCategory -> defaultToWildcard(searchTaskRequest.getWorkTypes()).stream()
                            .flatMap(workType -> defaultToWildcard(searchTaskRequest.getRegions()).stream()
                                .flatMap(region -> defaultToWildcard(searchTaskRequest.getLocations()).stream()
                                    .map(location ->
                                        String.join(
                                            ":",
                                            state,
                                            jurisdiction,
                                            roleCategory,
                                            workType,
                                            region,
                                            location))
                                )
                            )
                        )
                )
            )
            .collect(Collectors.toCollection(HashSet::new));  // Collecting into a HashSet
    }


    private static Collection<String> defaultToWildcard(Collection<String> strings) {
        return strings == null || strings.isEmpty() ? WILDCARD : strings;
    }
}
