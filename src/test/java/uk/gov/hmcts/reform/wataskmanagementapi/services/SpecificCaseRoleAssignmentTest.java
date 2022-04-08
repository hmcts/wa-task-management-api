package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.apache.commons.compress.utils.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.data.RoleAssignmentCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpecificCaseRoleAssignmentTest {

    @Test
    void run_scenario() {
        prepareRoleAssignments();
    }

    /*
    Select all role assignments that are specific, case and public
    Select all the role assignments that have the same rolename, jurisdiction, caseType
    Collect all the case_ids for the above matches
     */
    void prepareRoleAssignments() {

        List<RoleAssignment> caseManagerSpecificCaseRoles =
            RoleAssignmentCreator.aRoleAssignmentList(2,
                RoleType.CASE,
                GrantType.SPECIFIC,
                "case-manager",
                Classification.PUBLIC);
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);

        List<RoleAssignment> caseWorkerSpecificCaseRoles =
            RoleAssignmentCreator.aRoleAssignmentList(2,
                RoleType.CASE,
                GrantType.SPECIFIC,
                "caseworker",
                Classification.PUBLIC);
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);
        List<RoleAssignment> caseWorkerStandardOrgRoles =
            RoleAssignmentCreator.aRoleAssignmentList(2,
                RoleType.ORGANISATION,
                GrantType.STANDARD,
                "caseworker",
                Classification.PUBLIC);
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);

        List<RoleAssignment> allRoleAssignments = new ArrayList<>();
        allRoleAssignments.addAll(caseManagerSpecificCaseRoles);
        allRoleAssignments.addAll(caseWorkerSpecificCaseRoles);
        allRoleAssignments.addAll(caseWorkerStandardOrgRoles);

        //Split Case and Org roles
        Map<Boolean, List<RoleAssignment>> splitRoleAssignments = allRoleAssignments.stream()
            .collect(Collectors.partitioningBy(roleAssignment -> roleAssignment.getRoleType().equals(RoleType.CASE)));

        assertThat(splitRoleAssignments).hasSize(2);
        assertThat(splitRoleAssignments.get(Boolean.TRUE)).hasSize(4);
        assertThat(splitRoleAssignments.get(Boolean.TRUE))
            .map(RoleAssignment::getRoleType).contains(RoleType.CASE);
        assertThat(splitRoleAssignments.get(Boolean.FALSE))
            .map(RoleAssignment::getRoleType).contains(RoleType.ORGANISATION);

        //Add Org Roles as they are
        List<RoleAssignment> accumulativeList = new ArrayList<>(splitRoleAssignments.get(Boolean.FALSE));


        //Group the Case roles
        Map<String, List<RoleAssignment>> groupedCaseRoles = //TODO use RoleAssignmentForSearch instead
            splitRoleAssignments.get(Boolean.TRUE)
                .stream()
                .collect(Collectors.groupingBy(RoleAssignment::getRoleName));//TODO Use hashcode instead

        // Pick the first and collect the caseIds for a group
        List<RoleAssignment> represenatives = Lists.newArrayList();

        //Map<String, RoleAssignment> representatives = Maps.newHashMap();

        for (Map.Entry<String, List<RoleAssignment>> groupEntry : groupedCaseRoles.entrySet()) {
            Optional<RoleAssignment> maybeRepresentative =
                groupEntry.getValue()
                    .stream().findFirst();
            if (maybeRepresentative.isPresent()) {
                RoleAssignment representative = maybeRepresentative.get();

                updateGroupRepresentative(representative, groupEntry.getValue());

            }

        }


        //List<String> groupCaseIds = new ArrayList<>();
        //Map<String, List<String>> groupIdWithListOfCases = groupedCaseRoles
        //    .values()
        //    .stream()
        //    //.map(roleAssignments ->
        //    //    roleAssignments.stream()
        //    .reduce(groupCaseIds, (currentCaseList, element) -> currentCaseList, (strings, strings2) -> strings.addAll(strings2)
        //    );


        //Map<Integer, List<RoleAssignmentForSearch>> groups =
        //    allRoleAssignments.stream()
        //        //.filter(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.SPECIFIC) &&
        //        //                          roleAssignment.getRoleType().equals(RoleType.CASE) &&
        //        //                          roleAssignment.getClassification().equals(Classification.PUBLIC)
        //        //)
        //        .collect(Collectors.groupingBy(roleAssignment -> roleAssignment.hashCode()));
        //.reduce(new ArrayList<RoleAssignment>(),
        //    (partialRoleAssignmentList, nextRoleAssignment) -> partialRoleAssignmentList.getRoleName().equals(nextRoleAssignment.getRoleName()));

        accumulativeList.addAll(represenatives);
        assertThat(accumulativeList).hasSize(3);

    }

    public RoleAssignment updateGroupRepresentative(RoleAssignment groupRepresentative, List<RoleAssignment> group) {

        //TODO add all CaseIds you find from the group to the Set in RoleAssignmentsForSearch
        return groupRepresentative;

    }


}
