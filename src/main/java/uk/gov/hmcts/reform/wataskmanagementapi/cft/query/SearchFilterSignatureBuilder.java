package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                        for (String region : defaultToWildcard(searchTaskRequest.getLocations())) {
                            for (String location : defaultToWildcard(searchTaskRequest.getLocations())) {
                                filterSignatures.add(state
                                        + ":"
                                        + jurisdiction
                                        + ":"
                                        + roleCategory
                                        + ":"
                                        + workType
                                        + ":"
                                        + region
                                        + ":"
                                        + location);
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
        return states.stream()
            .filter(s -> s.equals(CFTTaskState.ASSIGNED) || s.equals(CFTTaskState.UNASSIGNED))
            .map(CFTTaskState::getValue)
            .map(s -> s.substring(0,1))
            .collect(Collectors.toSet());
    }

    private static Set<String> abbreviateRoleCategories(List<String> roleCategories) {
        Set<String> abbreviated = new HashSet<>();
        for (String roleCategory : roleCategories) {
            if (RoleCategory.JUDICIAL.name().equalsIgnoreCase(roleCategory)) {
                abbreviated.add("J");
            } else if (RoleCategory.LEGAL_OPERATIONS.name().equalsIgnoreCase(roleCategory)) {
                abbreviated.add("L");
            } else if (RoleCategory.ADMIN.name().equalsIgnoreCase(roleCategory)) {
                abbreviated.add("A");
            } else if (RoleCategory.UNKNOWN.name().equalsIgnoreCase(roleCategory)) {
                abbreviated.add("U");
            } else {
                abbreviated.add(roleCategory);
            }
        }
        return abbreviated;
    }
}
