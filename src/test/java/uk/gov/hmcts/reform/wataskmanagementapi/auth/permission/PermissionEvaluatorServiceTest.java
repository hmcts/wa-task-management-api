package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorServiceTest {

    private PermissionEvaluatorService permissionEvaluatorService;

    private Map<String, CamundaVariable> defaultVariables;

    @BeforeEach
    public void setUp() {
        permissionEvaluatorService = new PermissionEvaluatorService(new CamundaObjectMapper());
        defaultVariables = getDefaultVariables();
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_read_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_multiple_roles_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        boolean result = permissionEvaluatorService.hasAccess(
            defaultVariables,
            testCases,
            permissionsRequired
        );
        assertTrue(result);
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_read_permission_and_abort_early_return_true() {

        List<PermissionTypes> permissionsRequired = asList(PermissionTypes.READ, PermissionTypes.CANCEL);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_read_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        defaultVariables.put(
            "tribunal-caseworker",
            new CamundaVariable("Refer,Own,Manage,Cancel", "String")
        );
        defaultVariables.put(
            "senior-tribunal-caseworker",
            new CamundaVariable("Refer,Own,Manage,Cancel", "String")
        );
        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_public_but_task_is_public_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_classification_public_but_task_is_private_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PRIVATE", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_classification_public_but_task_is_restricted_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("RESTRICTED", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_private_and_task_is_public_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PRIVATE,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_private_and_task_is_private_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PRIVATE", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PRIVATE,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_restricted_but_task_is_public_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.RESTRICTED,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_restricted_but_task_is_private_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PRIVATE", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.RESTRICTED,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_classification_restricted_but_task_is_restricted_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("RESTRICTED", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.RESTRICTED,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_throw_IllegalArgumentException_when_looking_for_unknown_classification() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);
        defaultVariables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("RESTRICTED", "String"));

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.UNKNOWN,
            emptyMap()
        );

        assertThrows(IllegalArgumentException.class, () ->
            permissionEvaluatorService.hasAccess(
                defaultVariables,
                testCases,
                permissionsRequired
            ));
    }

    @ParameterizedTest
    @MethodSource("endTimeScenarioProvider")
    void hasAccess_should_succeed_when_looking_for_end_time_permission_and_return_true(EndTimeScenario scenario) {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        boolean actualHasAccess = permissionEvaluatorService.hasAccess(
            defaultVariables,
            singletonList(scenario.roleAssignment),
            permissionsRequired
        );

        assertEquals(scenario.expectedHasAccess, actualHasAccess);
    }

    private static Stream<EndTimeScenario> endTimeScenarioProvider() {

        EndTimeScenario endTimeIsNull = EndTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenEndTime(null))
            .expectedHasAccess(true)
            .build();

        EndTimeScenario endTimeIsAfterCurrentTime = EndTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenEndTime(LocalDateTime.now().minusDays(3)))
            .expectedHasAccess(false)
            .build();

        EndTimeScenario endTimeIsBeforeCurrentTime = EndTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenEndTime(LocalDateTime.now().plusDays(3)))
            .expectedHasAccess(true)
            .build();

        return Stream.of(
            endTimeIsNull,
            endTimeIsAfterCurrentTime,
            endTimeIsBeforeCurrentTime
        );
    }

    private static Assignment buildRoleAssignmentGivenEndTime(LocalDateTime endTime) {
        return Assignment.builder()
            .actorIdType(ActorIdType.IDAM)
            .actorId("some actor id")
            .roleType(RoleType.ORGANISATION)
            .roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .endTime(endTime)
            .build();
    }

    @Builder
    private static class EndTimeScenario {
        Assignment roleAssignment;
        boolean expectedHasAccess;
    }

    @ParameterizedTest
    @MethodSource("beginTimeScenarioProvider")
    void hasAccess_should_succeed_when_looking_for_begin_time_permission_and_return_true(
        BeginTimeScenario scenario) {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        boolean actualHasAccess = permissionEvaluatorService.hasAccess(
            defaultVariables,
            singletonList(scenario.roleAssignment),
            permissionsRequired
        );

        assertEquals(scenario.expectedHasAccess, actualHasAccess);
    }

    private static Stream<BeginTimeScenario> beginTimeScenarioProvider() {

        BeginTimeScenario beginTimeIsNull = BeginTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenBeginTime(null))
            .expectedHasAccess(true)
            .build();

        BeginTimeScenario beginTimeIsAfterCurrentTime = BeginTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenBeginTime(LocalDateTime.now().minusDays(3)))
            .expectedHasAccess(true)
            .build();

        BeginTimeScenario beginTimeIsBeforeCurrentTime = BeginTimeScenario.builder()
            .roleAssignment(buildRoleAssignmentGivenBeginTime(LocalDateTime.now().plusDays(3)))
            .expectedHasAccess(false)
            .build();

        return Stream.of(
            beginTimeIsNull,
            beginTimeIsAfterCurrentTime,
            beginTimeIsBeforeCurrentTime
        );
    }

    private static Assignment buildRoleAssignmentGivenBeginTime(LocalDateTime beginTime) {
        return Assignment.builder()
            .actorIdType(ActorIdType.IDAM)
            .actorId("some actor id")
            .roleType(RoleType.ORGANISATION)
            .roleName("tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .beginTime(beginTime)
            .build();
    }

    @Builder
    private static class BeginTimeScenario {
        Assignment roleAssignment;
        boolean expectedHasAccess;
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_jurisdiction_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.JURISDICTION.value(), "IA")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_jurisdiction_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.JURISDICTION.value(), "AnotherJurisdiction")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_case_id_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.CASE_ID.value(), "123456789")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_case_id_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.CASE_ID.value(), "AnotherCaseId")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }


    @Test
    void hasAccess_should_succeed_when_looking_for_region_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.REGION.value(), "1")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_region_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.REGION.value(), "anotherRegion")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }


    @Test
    void hasAccess_should_succeed_when_looking_for_location_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.PRIMARY_LOCATION.value(), "012345")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_location_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.BASE_LOCATION.value(), "anotherLocationId")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_caseTypeId_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.CASE_TYPE.value(), "Asylum")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_caseTypeId_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<Assignment> testCases = createTestAssignments(
            asList("tribunal-caseworker", "senior-tribunal-caseworker"),
            Classification.PUBLIC,
            Map.of(RoleAttributeDefinition.CASE_TYPE.value(), "invalidAsylum")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccess(
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    private List<Assignment> createTestAssignments(List<String> roleNames,
                                                   Classification roleClassification,
                                                   Map<String, String> roleAttributes) {

        List<Assignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                    Assignment roleAssignment = createBaseAssignment(
                        UUID.randomUUID().toString(),
                        "tribunal-caseworker",
                        roleType,
                        roleClassification,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }
            ));
        return allTestRoles;
    }

    private Assignment createBaseAssignment(String actorId,
                                            String roleName,
                                            RoleType roleType,
                                            Classification classification,
                                            Map<String, String> attributes) {
        return new Assignment(
            ActorIdType.IDAM,
            actorId,
            roleType,
            roleName,
            classification,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            attributes
        );
    }

    private Map<String, CamundaVariable> getDefaultVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put(CASE_ID.value(), new CamundaVariable("123456789", "String"));
        variables.put(CASE_NAME.value(), new CamundaVariable("someCaseName", "String"));
        variables.put(CASE_TYPE_ID.value(), new CamundaVariable("Asylum", "String"));
        variables.put(TASK_STATE.value(), new CamundaVariable("configured", "String"));
        variables.put(LOCATION.value(), new CamundaVariable("012345", "String"));
        variables.put(LOCATION_NAME.value(), new CamundaVariable("A Hearing Centre", "String"));
        variables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));
        variables.put(REGION.value(), new CamundaVariable("1", "String"));
        variables.put(JURISDICTION.value(), new CamundaVariable("IA", "String"));
        variables.put(
            "tribunal-caseworker",
            new CamundaVariable("Read,Refer,Own,Manage,Cancel", "String")
        );
        variables.put(
            "senior-tribunal-caseworker",
            new CamundaVariable("Read,Refer,Own,Manage,Cancel", "String")
        );

        return variables;
    }

}
