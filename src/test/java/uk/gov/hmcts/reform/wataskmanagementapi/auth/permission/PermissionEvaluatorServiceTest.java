package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CCD_ID;
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
            Map.of(RoleAttributeDefinition.REGION.value(), "east-england")
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
            Map.of(RoleAttributeDefinition.PRIMARY_LOCATION.value(), "anotherLocationId")
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
            RoleCategory.STAFF,
            false,
            attributes
        );
    }

    private Map<String, CamundaVariable> getDefaultVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put(CCD_ID.value(), new CamundaVariable("123456789", "String"));
        variables.put(CASE_NAME.value(), new CamundaVariable("someCaseName", "String"));
        variables.put(CASE_TYPE_ID.value(), new CamundaVariable("someCaseType", "String"));
        variables.put(TASK_STATE.value(), new CamundaVariable("configured", "String"));
        variables.put(LOCATION.value(), new CamundaVariable("012345", "String"));
        variables.put(LOCATION_NAME.value(), new CamundaVariable("A Hearing Centre", "String"));
        variables.put(SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));
        variables.put(REGION.value(), new CamundaVariable("east-england", "String"));
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
