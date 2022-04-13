package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import com.launchdarkly.shaded.com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        List<RoleAssignmentForSearch> caseRoleAssignments = roleAssignmentsByRoleType.get(RoleType.CASE.name());
        List<RoleAssignmentForSearch> representatives = Lists.newArrayList();

        if (!CollectionUtils.isEmpty(caseRoleAssignments)) {

            //Group the Case roles
            Map<Integer, List<RoleAssignmentForSearch>> groupedCaseRoles = caseRoleAssignments
                .stream()
                .collect(Collectors.groupingBy(RoleAssignmentForSearch::hashCode));

            // Pick the first and collect the caseIds for a group

            for (Map.Entry<Integer, List<RoleAssignmentForSearch>> groupEntry : groupedCaseRoles.entrySet()) {
                Optional<RoleAssignmentForSearch> maybeRepresentative = groupEntry.getValue().stream().findFirst();
                if (maybeRepresentative.isPresent()) {
                    RoleAssignmentForSearch representative = maybeRepresentative.get();
                    updateGroupRepresentative(representative, groupEntry.getValue());
                    representatives.add(representative);
                }
            }
        }
        //Add Org Roles as they are
        List<RoleAssignmentForSearch> roleAssignmentForOrganisation
            = roleAssignmentsByRoleType.get(RoleType.ORGANISATION.name());

        List<RoleAssignmentForSearch> finalRoleAssignments = new ArrayList<>();
        if (!CollectionUtils.isEmpty(roleAssignmentForOrganisation)) {
            finalRoleAssignments.addAll(roleAssignmentForOrganisation);
        }
        if (!CollectionUtils.isEmpty(representatives)) {
            finalRoleAssignments.addAll(representatives);
        }

        return finalRoleAssignments;
    }

    private RoleAssignmentForSearch updateGroupRepresentative(RoleAssignmentForSearch groupRepresentative,
                                                              List<RoleAssignmentForSearch> group) {

        for (RoleAssignmentForSearch groupMember : group) {
            groupRepresentative.getCaseIds().addAll(groupMember.getCaseIds());
        }

        return groupRepresentative;

    }
}
