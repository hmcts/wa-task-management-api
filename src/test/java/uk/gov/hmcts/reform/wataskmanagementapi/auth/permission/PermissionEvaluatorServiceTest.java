package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
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

    private static RoleAssignment buildRoleAssignmentGivenEndTime(LocalDateTime endTime) {
        return RoleAssignment.builder()
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

    private static RoleAssignment buildRoleAssignmentGivenBeginTime(LocalDateTime beginTime) {
        return RoleAssignment.builder()
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

    @BeforeEach
    public void setUp() {
        CamundaObjectMapper camundaObjectMapper = new CamundaObjectMapper();
        permissionEvaluatorService = new PermissionEvaluatorService(
            camundaObjectMapper,
            new AttributesValueVerifier(camundaObjectMapper)
        );
        defaultVariables = getDefaultVariables();
    }

    @Test
    void hasAccessWithUserIdAssigneeCheck_should_succeed_senior_tribunal_case_worker() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                null,
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccessWithUserIdAssigneeCheck_should_succeed_senior_tribunal_case_worker_another_user_assigned() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    @Test
    void hasAccessWithUserIdAssigneeCheck_should_succeed_tribunal_case_worker_assigned_to_task() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
            singletonList("tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "someUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }


    @Test
    void hasAccessWithUserIdAssigneeCheck_should_fail_different_tribunal_case_worker_assigned_to_task() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
            singletonList("tribunal-caseworker"),
            Classification.PUBLIC,
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_read_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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
        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

    @Test
    void hasAccess_should_succeed_when_looking_for_jurisdiction_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

        List<RoleAssignment> testCases = createTestAssignments(
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

    @Test
    void hasAccess_should_fail_when_looking_for_begin_time_greater_than_today_and_return_false() {
        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.OWN);

        List<RoleAssignment> testCases = createTestAssignmentsEdgeCase(
            ActorIdType.IDAM,
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            LocalDateTime.now().plusYears(1),
            LocalDateTime.now().plusYears(2),
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_end_time_less_than_today_and_return_false() {
        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.OWN);

        List<RoleAssignment> testCases = createTestAssignmentsEdgeCase(
            ActorIdType.IDAM,
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            LocalDateTime.now().minusYears(1),
            LocalDateTime.now().minusYears(2),
            emptyMap()
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_different_location() {
        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.OWN);

        List<RoleAssignment> testCases = createTestAssignmentsEdgeCase(
            ActorIdType.IDAM,
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            LocalDateTime.now().minusYears(1),
            LocalDateTime.now().plusYears(2),
            Map.of(RoleAttributeDefinition.BASE_LOCATION.value(), "location")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertFalse(result);
        });
    }

    @Test
    void hasAccess_should_success_when_looking_for_correct_location() {
        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.OWN);

        List<RoleAssignment> testCases = createTestAssignmentsEdgeCase(
            ActorIdType.IDAM,
            singletonList("senior-tribunal-caseworker"),
            Classification.PUBLIC,
            GrantType.SPECIFIC,
            RoleCategory.LEGAL_OPERATIONS,
            false,
            LocalDateTime.now().minusYears(1),
            LocalDateTime.now().plusYears(2),
            Map.of(RoleAttributeDefinition.BASE_LOCATION.value(), "012345")
        );

        testCases.forEach(roleAssignment -> {
            boolean result = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                "anotherUserId",
                "someUserId",
                defaultVariables,
                singletonList(roleAssignment),
                permissionsRequired
            );
            assertTrue(result);
        });
    }

    private List<RoleAssignment> createTestAssignments(List<String> roleNames,
                                                       Classification roleClassification,
                                                       Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                    RoleAssignment roleAssignment = createBaseAssignment(
                        UUID.randomUUID().toString(),
                        roleName,
                        roleType,
                        roleClassification,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }
            ));
        return allTestRoles;
    }

    private RoleAssignment createBaseAssignment(String actorId,
                                                String roleName,
                                                RoleType roleType,
                                                Classification classification,
                                                Map<String, String> attributes) {
        return new RoleAssignment(
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

    private List<RoleAssignment> createTestAssignmentsEdgeCase(ActorIdType actorIdType,
                                                               List<String> roleNames,
                                                               Classification roleClassification,
                                                               GrantType grantType,
                                                               RoleCategory roleCategory,
                                                               boolean readOnly,
                                                               LocalDateTime beginTime,
                                                               LocalDateTime endTime,
                                                               Map<String, String> roleAttributes) {

        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                    RoleAssignment roleAssignment = createBaseAssignmentEdgeCase(actorIdType,
                        UUID.randomUUID().toString(),
                        roleType,
                        roleName,
                        roleClassification,
                        grantType,
                        roleCategory,
                        readOnly,
                        beginTime,
                        endTime,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }
            ));
        return allTestRoles;
    }

    private RoleAssignment createBaseAssignmentEdgeCase(ActorIdType actorIdType,
                                                        String actorId,
                                                        RoleType roleType,
                                                        String roleName,
                                                        Classification classification,
                                                        GrantType grantType,
                                                        RoleCategory roleCategory,
                                                        boolean readOnly,
                                                        LocalDateTime beginTime,
                                                        LocalDateTime endTime,
                                                        Map<String, String> attributes
    ) {
        return RoleAssignment.builder()
            .actorIdType(actorIdType)
            .actorId(actorId)
            .roleType(roleType)
            .roleName(roleName)
            .classification(classification)
            .grantType(grantType)
            .roleCategory(roleCategory)
            .readOnly(readOnly)
            .beginTime(beginTime)
            .endTime(endTime)
            //.created()
            .attributes(attributes)
            //.authorisations()
            .build();
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

    @Builder
    private static class EndTimeScenario {
        RoleAssignment roleAssignment;
        boolean expectedHasAccess;
    }

    @Builder
    private static class BeginTimeScenario {
        RoleAssignment roleAssignment;
        boolean expectedHasAccess;
    }

}
