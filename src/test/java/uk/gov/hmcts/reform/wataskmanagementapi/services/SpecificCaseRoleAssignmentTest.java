package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.apache.commons.collections.map.HashedMap;
import org.apache.groovy.util.Maps;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.CaseRoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            aRoleAssignmentList(2, RoleType.CASE, GrantType.SPECIFIC, "case-manager");
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);
        List<RoleAssignment> caseWorkerSpecificCaseRoles =
            aRoleAssignmentList(2, RoleType.CASE, GrantType.SPECIFIC, "caseworker");
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);
        List<RoleAssignment> caseWorkerStandardOrgRoles =
            aRoleAssignmentList(2, RoleType.ORGANISATION, GrantType.STANDARD, "caseworker");
        assertThat(caseManagerSpecificCaseRoles.size()).isEqualTo(2);


        List<CaseRoleAssignmentForSearch> accumulativeList = new ArrayList<>();

        List<RoleAssignment> combinedRolesList = new ArrayList<>();
        combinedRolesList.addAll(caseManagerSpecificCaseRoles);
        combinedRolesList.addAll(caseWorkerSpecificCaseRoles);
        combinedRolesList.addAll(caseWorkerStandardOrgRoles);

        accumulativeList.addAll(caseWorkerStandardOrgRoles);

        List<String> caseIds = new ArrayList<>();

        Map<Integer, List<CaseRoleAssignmentForSearch>> groups =
            combinedRolesList.stream()
                //.filter(roleAssignment -> roleAssignment.getGrantType().equals(GrantType.SPECIFIC) &&
                //                          roleAssignment.getRoleType().equals(RoleType.CASE) &&
                //                          roleAssignment.getClassification().equals(Classification.PUBLIC)
                //)
        .collect(Collectors.groupingBy(roleAssignment -> roleAssignment.hashCode()));
        //.reduce(new ArrayList<RoleAssignment>(),
        //    (partialRoleAssignmentList, nextRoleAssignment) -> partialRoleAssignmentList.getRoleName().equals(nextRoleAssignment.getRoleName()));



        //getrespresentative
        accumulativeList.addAll(represenatives);

        return accumulativeList;

        assertThat(groups).hasSize(3);

    }

    class RoleAssignmentCreator

    public static List<RoleAssignment> aRoleAssignment(RoleType roleType, GrantType grantType, String roleName) {

        return new RoleAssignment(ActorIdType.IDAM,
            "actorId" +
            roleType,
            roleName,
            Classification.PUBLIC,
            grantType,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            Maps.of(
                "jurisdiction", "IA",
                "caseId", "caseId:"));

    }

    public static List<RoleAssignment> aRoleAssignmentList(int size) {

        List<RoleAssignment> roles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {

            aRoleAssignment()

            roles.add(new RoleAssignment(ActorIdType.IDAM,
                "actorId" + i,
                roleType,
                roleName,
                Classification.PUBLIC,
                grantType,
                RoleCategory.LEGAL_OPERATIONS,
                false,
                Maps.of(
                    "jurisdiction", "IA",
                    "caseId", "caseId:" + i))
            );

        }

        return roles;

    }


}
