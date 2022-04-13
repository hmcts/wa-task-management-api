package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.Builder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignmentForSearch;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentSearchData;
import uk.gov.hmcts.reform.wataskmanagementapi.data.RoleAssignmentCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SpecificCaseRoleAssignmentTest {
    
    @ParameterizedTest
    @MethodSource({
        "performanceTestBasedScenarios",
        "mixedRoleTypeBasedScenarios",
        "organisationalRoleTypeBasedScenarios",
        "caseRoleTypeBasedScenarios"
    })
    void should_group_case_roles_assignments_for_search(RoleAssignmentForSearchScenario scenarios) {
        //Create some data and check
        List<RoleAssignment> allRoleAssignments = createRoleAssignmentData(scenarios.roleAssignmentExpectations);
        assertThat(allRoleAssignments).hasSize(scenarios.totalRoleAssignmentsForScenarios);

        //Split Case and Org roles

        RoleAssignmentSearchData roleAssignmentSearchData = new RoleAssignmentSearchData(
            allRoleAssignments,
            RoleAssignmentForSearch::getRoleType
        );

        List<RoleAssignmentForSearch> roleAssignmentsForSearch = roleAssignmentSearchData.getRoleAssignmentsForSearch();
        assertThat(roleAssignmentsForSearch).hasSize(scenarios.expectedResponses);

        Map<String, List<RoleAssignmentForSearch>> roleAssignmentsByRoleTypes
            = roleAssignmentsForSearch.stream()
            .collect(Collectors.groupingBy(RoleAssignmentForSearch::getRoleType));

        if (scenarios.expectedOrganisationRoles != 0) {
            assertThat(roleAssignmentsByRoleTypes.get(RoleType.ORGANISATION.name()))
                .hasSize(scenarios.expectedOrganisationRoles);
        }
        if (scenarios.expectedCaseRoles != 0) {
            assertThat(roleAssignmentsByRoleTypes.get(RoleType.CASE.name())).hasSize(scenarios.expectedCaseRoles);
        }
    }

    private static Stream<RoleAssignmentForSearchScenario> performanceTestBasedScenarios() {
        return Stream.of(
            RoleAssignmentForSearchScenario.builder()
                .roleAssignmentExpectations(List.of(
                    RoleAssignmentExpectations.builder()
                        .roleName("case-manager")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(172)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("case-allocator")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hmcts-legal-operations")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("task-supervisor")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("tribunal-caseworker")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build()
                ))
                .totalRoleAssignmentsForScenarios(176)
                .expectedResponses(5)
                .expectedCaseRoles(1)
                .expectedOrganisationRoles(4)
                .build()
        );
    }

    private static Stream<RoleAssignmentForSearchScenario> mixedRoleTypeBasedScenarios() {
        return Stream.of(
            RoleAssignmentForSearchScenario.builder()
                .roleAssignmentExpectations(List.of(
                    RoleAssignmentExpectations.builder()
                        .roleName("case-manager")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("case-allocator")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("tribunal-caseworker")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hearing-judge")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("task-supervisor")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("tribunal-caseworker")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of())
                        .build()
                ))
                .totalRoleAssignmentsForScenarios(12)
                .expectedResponses(8)
                .expectedCaseRoles(4)
                .expectedOrganisationRoles(4)
                .build()
        );
    }

    private static Stream<RoleAssignmentForSearchScenario> caseRoleTypeBasedScenarios() {
        return Stream.of(
            RoleAssignmentForSearchScenario.builder()
                .roleAssignmentExpectations(List.of(
                    RoleAssignmentExpectations.builder()
                        .roleName("case-manager")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("case-allocator")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("tribunal-caseworker")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hearing-judge")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("additional-attributes-requested")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("specific-access-requested")
                        .roleType(RoleType.CASE)
                        .grantType(GrantType.SPECIFIC)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(2)
                        .authorisations(List.of("371"))
                        .build()
                ))
                .totalRoleAssignmentsForScenarios(12)
                .expectedResponses(6)
                .expectedCaseRoles(6)
                .expectedOrganisationRoles(0)
                .build()
        );
    }

    private static Stream<RoleAssignmentForSearchScenario> organisationalRoleTypeBasedScenarios() {
        return Stream.of(
            RoleAssignmentForSearchScenario.builder()
                .roleAssignmentExpectations(List.of(
                    RoleAssignmentExpectations.builder()
                        .roleName("task-supervisor")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hearing-centre-admin")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("senior-tribunal-caseworker")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hmcts-admin")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("hmcts-legal-operations")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build(),
                    RoleAssignmentExpectations.builder()
                        .roleName("tribunal-caseworker")
                        .roleType(RoleType.ORGANISATION)
                        .grantType(GrantType.STANDARD)
                        .classification(Classification.PUBLIC)
                        .numberOfRoles(1)
                        .authorisations(List.of())
                        .build()
                ))
                .totalRoleAssignmentsForScenarios(6)
                .expectedResponses(6)
                .expectedCaseRoles(0)
                .expectedOrganisationRoles(6)
                .build()
        );
    }

    public List<RoleAssignment> createRoleAssignmentData(List<RoleAssignmentExpectations> scenarios) {

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        for (RoleAssignmentExpectations scenario : scenarios) {
            roleAssignments.addAll(
                RoleAssignmentCreator.aRoleAssignmentList(
                    scenario.numberOfRoles,
                    scenario.roleType,
                    scenario.grantType,
                    scenario.roleName,
                    scenario.classification,
                    scenario.authorisations
                ));
        }
        return roleAssignments;
    }

    @Builder
    private static class RoleAssignmentForSearchScenario {
        List<RoleAssignmentExpectations> roleAssignmentExpectations;
        int totalRoleAssignmentsForScenarios;
        int expectedResponses;
        int expectedOrganisationRoles;
        int expectedCaseRoles;
    }

    @Builder
    private static class RoleAssignmentExpectations {
        String roleName;
        RoleType roleType;
        GrantType grantType;
        Classification classification;
        int numberOfRoles;
        List<String> authorisations;
    }
}
