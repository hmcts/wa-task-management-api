package uk.gov.hmcts.reform.wataskmanagementapi.services.signature;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({
    "PMD.CognitiveComplexity"})
public final class SearchFilterSignatureBuilder {
    private static final Set<String> WILDCARD = Collections.singleton("*");

    private SearchFilterSignatureBuilder() {
        //Utility class constructor
    }

    public static Set<String> buildFilterSignatures(SearchRequest searchTaskRequest) {
        Set<String> filterSignatures = new HashSet<>();
        for (String state : defaultToWildcard(CFTTaskState.getAbbreviations(searchTaskRequest.getCftTaskStates()))) {
            for (String jurisdiction : defaultToWildcard(searchTaskRequest.getJurisdictions())) {
                for (String roleCategory :
                    defaultToWildcard(RoleCategory.getAbbreviations(searchTaskRequest.getRoleCategories()))) {
                    for (String workType : defaultToWildcard(searchTaskRequest.getWorkTypes())) {
                        for (String region : defaultToWildcard(searchTaskRequest.getRegions())) {
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
}
