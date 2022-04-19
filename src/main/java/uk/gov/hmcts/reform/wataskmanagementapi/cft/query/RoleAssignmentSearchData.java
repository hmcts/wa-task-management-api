package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RoleAssignmentSearchData {

    private final List<RoleAssignment> roleAssignments;
    private final Function<RoleAssignmentForSearch, String> groupingStrategy;

    public RoleAssignmentSearchData(List<RoleAssignment> roleAssignments,
                                    Function<RoleAssignmentForSearch, String> groupingStrategy) {
        this.roleAssignments = roleAssignments;
        this.groupingStrategy = groupingStrategy;
    }

    public List<RoleAssignmentForSearch> getRoleAssignmentsForSearch() {
        Map<String, List<RoleAssignmentForSearch>> roleAssignmentsByRoleType = roleAssignments.stream()
            .map(RoleAssignmentForSearch::new)
            .collect(Collectors.toList()).stream()
            .collect(Collectors.groupingBy(groupingStrategy));

        //Add Org Roles as they are
        List<RoleAssignmentForSearch> roleAssignmentForOrganisation
            = roleAssignmentsByRoleType.get(RoleType.ORGANISATION.name());

        List<RoleAssignmentForSearch> finalRoleAssignments = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roleAssignmentForOrganisation)) {
            finalRoleAssignments.addAll(roleAssignmentForOrganisation);
        }

        List<RoleAssignmentForSearch> caseRoleAssignments = roleAssignmentsByRoleType.get(RoleType.CASE.name());
        List<RoleAssignmentForSearch> caseRepresentatives = getCaseRepresentatives(caseRoleAssignments);
        if (!CollectionUtils.isEmpty(caseRepresentatives)) {
            finalRoleAssignments.addAll(caseRepresentatives);
        }

        return finalRoleAssignments;
    }

    private List<RoleAssignmentForSearch> getCaseRepresentatives(List<RoleAssignmentForSearch> caseRoleAssignments) {
        if (CollectionUtils.isEmpty(caseRoleAssignments)) {
            return Collections.emptyList();
        }

        //Group the Case roles
        Map<Integer, List<RoleAssignmentForSearch>> groupedCaseRoles = caseRoleAssignments
            .stream()
            .collect(Collectors.groupingBy(RoleAssignmentForSearch::hashCode));

        return groupedCaseRoles.values().stream()
            .filter(caseRoles -> !CollectionUtils.isEmpty(caseRoles))
            .map(roleAssignmentForSearches -> {
                RoleAssignmentForSearch representative = roleAssignmentForSearches.get(0);
                updateGroupRepresentative(representative, roleAssignmentForSearches);
                return representative;
            })
            .collect(Collectors.toList());
    }

    private RoleAssignmentForSearch updateGroupRepresentative(RoleAssignmentForSearch groupRepresentative,
                                                              List<RoleAssignmentForSearch> group) {

        for (RoleAssignmentForSearch groupMember : group) {
            groupRepresentative.getCaseIds().addAll(groupMember.getCaseIds());
        }

        return groupRepresentative;

    }
}
