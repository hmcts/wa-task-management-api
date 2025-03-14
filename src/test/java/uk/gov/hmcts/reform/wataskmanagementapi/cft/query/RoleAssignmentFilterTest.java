package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.inActiveRoles;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithAllGrantTypes;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithChallengedGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithStandardGrantType;


@ExtendWith(MockitoExtension.class)
class RoleAssignmentFilterTest {

    @Mock
    private Root<TaskResource> root;
    @Mock
    private CriteriaBuilder builder;
    @Mock
    private Predicate permissionsPredicate;
    @Mock
    private Predicate equalPredicate;
    @Mock
    private Predicate authorizationsPredicate;
    @Mock
    private CriteriaBuilder.In<Object> inObject;
    @Mock
    private Join<Object, Object> taskRoleResources;
    @Mock
    private Path<Object> pathObject;
    @Mock
    private Path<Object> roleNamePath;
    @Mock
    private Path<Object> classificationPath;

    @BeforeEach
    void setUp() {
        lenient().when(root.join("taskRoleResources")).thenReturn(taskRoleResources);
        lenient().when(taskRoleResources.get("read")).thenReturn(pathObject);
        lenient().when(builder.or(any())).thenReturn(inObject);
        lenient().when(builder.or()).thenReturn(inObject);
        lenient().when(builder.or(any(), any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any(), any(), any(), any(), any(), any())).thenReturn(inObject);
        lenient().when(builder.isTrue(any())).thenReturn(permissionsPredicate);
        lenient().when(builder.in(any())).thenReturn(inObject);
    }

    @ParameterizedTest
    @EnumSource(value = Classification.class, names = {"PUBLIC"})
    void should_build_query_for_specific(Classification classification) {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);

        lenient().when(root.get("securityClassification")).thenReturn(pathObject);
        lenient().when(root.get("caseTypeId")).thenReturn(pathObject);
        lenient().when(root.get("caseId")).thenReturn(pathObject);
        lenient().when(root.get("jurisdiction")).thenReturn(pathObject);

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        lenient().when(builder.equal(pathObject, "Asylum")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "IA")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "1623278362431003")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "hmcts-judiciary")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "senior-tribunal-caseworker")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, new String[]{})).thenReturn(equalPredicate);

        Predicate predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignmentWithSpecificGrantType(classification), builder, root);

        assertNotNull(builder);
        assertNotNull(predicate);

        Mockito.verify(builder, Mockito.times(1)).equal(pathObject, "senior-tribunal-caseworker");
        verify(builder, times(1)).equal(pathObject, "hmcts-judiciary");
        verify(builder, times(2)).equal(pathObject, new String[]{});
        verify(builder, times(1)).equal(pathObject, "Asylum");
        verify(builder, times(1)).equal(pathObject, "IA");
        verify(builder, times(1)).equal(pathObject, "1623278362431003");

        verify(root, times(1)).join(anyString());
        verify(root, times(5)).get(anyString());
        verify(pathObject, times(2)).isNull();
        verify(builder, times(5)).equal(any(), anyString());
        verify(builder, times(3)).or(any(Predicate[].class));
        verify(builder, times(3)).or(any(), any());
        verify(builder, times(4)).and(any(), any());
        verify(builder, times(2)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(7)).conjunction();
    }

    @ParameterizedTest
    @EnumSource(value = Classification.class, names = {"PUBLIC"})
    void should_build_query_for_specific_when_attributes_does_not_match(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .roleType(RoleType.ORGANISATION)
            .classification(classification)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        when(taskRoleResources.get("roleName")).thenReturn(roleNamePath);

        Predicate roleNamePredicate = mock(Predicate.class);
        when(builder.equal(roleNamePath, "hmcts-judiciary")).thenReturn(roleNamePredicate);

        lenient().when(root.get("securityClassification")).thenReturn(classificationPath);

        when(builder.equal(classificationPath, SecurityClassification.PUBLIC))
            .thenReturn(inObject);

        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        Predicate predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignments, builder, root);

        assertNotNull(builder);
        assertNotNull(predicate);

        when(taskRoleResources.get("roleName")).thenReturn(roleNamePath);

        roleNamePredicate = mock(Predicate.class);
        when(builder.equal(roleNamePath, "hmcts-judiciary")).thenReturn(roleNamePredicate);

        lenient().when(root.get("securityClassification")).thenReturn(classificationPath);

        when(builder.equal(classificationPath, SecurityClassification.PUBLIC))
            .thenReturn(null);

        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(null);

        predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignments, builder, root);

        assertNotNull(builder);
        assertNotNull(predicate);
    }

    @Test
    void should_build_query_for_specific_with_out_attributes() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        Predicate predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse.getRoleAssignments(), builder, root);
        assertNotNull(predicate);
    }

    @ParameterizedTest
    @EnumSource(value = Classification.class, names = {"PUBLIC", "PRIVATE", "RESTRICTED"})
    void should_build_query_for_standard_and_excluded(Classification classification) {
        lenient().when(root.get("securityClassification")).thenReturn(pathObject);
        lenient().when(root.get("caseTypeId")).thenReturn(pathObject);
        lenient().when(root.get("caseId")).thenReturn(pathObject);
        lenient().when(root.get("jurisdiction")).thenReturn(pathObject);
        lenient().when(root.get("region")).thenReturn(pathObject);
        lenient().when(root.get("location")).thenReturn(pathObject);

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        lenient().when(builder.equal(pathObject, "Asylum")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "IA")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "1623278362431003")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "1")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "765324")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "hmcts-judiciary")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "tribunal-caseworker")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, new String[]{})).thenReturn(equalPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignmentWithStandardGrantType(classification), builder, root);

        verify(builder, times(1)).equal(pathObject, new String[]{});
        verify(builder, times(1)).equal(pathObject, "Asylum");
        verify(builder, times(1)).equal(pathObject, "IA");
        verify(builder, times(1)).equal(pathObject, "1623278362431003");
        verify(builder, times(1)).equal(pathObject, "1");
        verify(builder, times(1)).equal(pathObject, "765324");

        verify(root, times(1)).join(anyString());
        verify(root, times(7)).get(anyString());
        verify(pathObject, times(1)).isNull();
        verify(builder, times(3)).or(any(Predicate[].class));
        verify(builder, times(2)).or(any(), any());
        verify(builder, times(4)).and(any(), any());
        verify(builder, times(1)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(1)).conjunction();
    }

    @ParameterizedTest
    @EnumSource(value = Classification.class, names = {"PUBLIC", "PRIVATE", "RESTRICTED"})
    void should_build_query_for_challenged_and_excluded(Classification classification) {
        lenient().when(root.get("securityClassification")).thenReturn(pathObject);
        lenient().when(root.get("caseTypeId")).thenReturn(pathObject);
        lenient().when(root.get("caseId")).thenReturn(pathObject);
        lenient().when(root.get("jurisdiction")).thenReturn(pathObject);

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        lenient().when(builder.equal(pathObject, "Asylum")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "IA")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "1623278362431003")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "hmcts-judiciary")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, "tribunal-caseworker")).thenReturn(equalPredicate);
        lenient().when(builder.equal(pathObject, new String[]{"DIVORCE", "PROBATE"})).thenReturn(equalPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignmentWithChallengedGrantType(classification), builder, root);

        verify(builder, times(1)).equal(pathObject, new String[]{});
        verify(builder, times(1)).equal(pathObject, "Asylum");
        verify(builder, times(1)).equal(pathObject, "IA");
        verify(builder, times(1)).equal(pathObject, "1623278362431003");

        verify(root, times(1)).join(anyString());
        verify(root, times(6)).get(anyString());
        verify(builder, times(2)).or(any());
        verify(builder, times(1)).or();
        verify(builder, times(3)).or(any(), any());
        verify(builder, times(4)).and(any(), any());
        verify(builder, times(1)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(2)).conjunction();
        if (classification != Classification.PUBLIC) {
            verify(builder, times(2)).in(any());
        }
    }

    @Test
    void should_build_query_for_all_grant_types() {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        Predicate predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignmentWithAllGrantTypes(), builder, root);

        assertNotNull(predicate);
    }

    @Test
    void should_build_query_for_negation_values() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(null)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .classification(Classification.PUBLIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);
        lenient().when(root.get("securityClassification")).thenReturn(null);
        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignments, builder, root);

        verify(builder, times(1)).equal(pathObject, "hmcts-judiciary");
        verify(builder, times(1)).equal(pathObject, new String[]{});

        verify(root, times(1)).join(anyString());
        verify(root, times(1)).get(anyString());
        verify(pathObject, times(1)).isNull();
        verify(builder, times(1)).or(any());
        verify(builder, times(2)).or();
        verify(builder, times(2)).or(any(), any());
        verify(builder, times(3)).and(any(), any());
        verify(builder, times(1)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(5)).conjunction();
    }

    @Test
    void should_build_query_for_invalid_classification() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(Classification.UNKNOWN)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);
        lenient().when(root.get("securityClassification")).thenReturn(null);
        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, roleAssignments, builder, root);

        verify(builder, times(1)).equal(pathObject, "hmcts-judiciary");
        verify(builder, times(1)).equal(pathObject, new String[]{});

        verify(root, times(1)).join(anyString());
        verify(root, times(1)).get(anyString());
        verify(pathObject, times(1)).isNull();
        verify(builder, times(1)).in(any());
        verify(builder, times(3)).or(any(Predicate[].class));
        verify(builder, times(2)).or(any(), any());
        verify(builder, times(3)).and(any(), any());
        verify(builder, times(1)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(5)).conjunction();
    }

    @Test
    void should_build_query_for_when_begin_and_end_time_are_active() {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleType(PermissionTypes.READ);
        RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, inActiveRoles(), builder, root);

        verify(builder, times(0)).equal(any(), any());
        verify(root, times(1)).join(anyString());
        verify(builder, times(1)).or(any(), any());
        verify(builder, times(0)).and(
            any(), any(), any(), any(), any(), any(), any());
        verify(builder, times(3)).or();
    }

    @Test
    void should_build_query_to_retrieve_role_information() {
        when(taskRoleResources.get("roleName")).thenReturn(roleNamePath);
        when(builder.equal(roleNamePath, "hmcts-judiciary")).thenReturn(equalPredicate);

        RoleAssignmentFilter.buildQueryToRetrieveRoleInformation(
            roleAssignmentWithSpecificGrantType(Classification.PUBLIC), builder, root);

        verify(root, times(1)).join(anyString());
        verify(builder, times(0)).equal(any(), any());
        verify(builder, times(1)).or(any(Predicate[].class));
        verify(builder, times(2)).and(any(), any());
    }

    @Test
    void should_build_query_to_retrieve_role_information_when_role_assignment_are_empty() {
        RoleAssignmentFilter.buildQueryToRetrieveRoleInformation(
            Collections.emptyList(), builder, root);

        verify(root, times(1)).join("taskRoleResources");
        verify(builder, times(1)).or(any(Predicate[].class));
        verify(builder, never()).equal(any(), any());
        verify(builder, never()).and(any(), any());
    }

    @Test
    void should_build_query_to_retrieve_role_information_when_role_assignment_are_inactive() {
        RoleAssignmentFilter.buildQueryToRetrieveRoleInformation(
            inActiveRoles(), builder, root);

        verify(root, times(1)).join("taskRoleResources");
        verify(builder, times(1)).or();
        verify(builder, never()).equal(any(), any());
        verify(builder, never()).and(any(), any());
    }

    @Test
    void should_build_query_for_permission_requirement_collection() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );

        lenient().when(taskRoleResources.get("roleName")).thenReturn(pathObject);
        lenient().when(taskRoleResources.get("authorizations")).thenReturn(pathObject);
        lenient().when(pathObject.isNull()).thenReturn(authorizationsPredicate);
        lenient().when(builder.and(new Predicate[]{permissionsPredicate, permissionsPredicate}))
            .thenReturn(permissionsPredicate);
        lenient().when(builder.or(permissionsPredicate, permissionsPredicate)).thenReturn(permissionsPredicate);

        PermissionRequirements requirements = new PermissionRequirementBuilder()
            .initPermissionRequirement(List.of(CLAIM, OWN), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(List.of(CLAIM, EXECUTE), PermissionJoin.AND)
            .joinPermissionRequirement(PermissionJoin.OR)
            .nextPermissionRequirement(List.of(ASSIGN, OWN), PermissionJoin.AND)
            .build();
        Predicate predicate = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            requirements, accessControlResponse.getRoleAssignments(), builder, root);
        assertNotNull(predicate);

        InOrder inOrder = inOrder(taskRoleResources);
        inOrder.verify(taskRoleResources).get(CLAIM.value().toLowerCase(Locale.ROOT));
        inOrder.verify(taskRoleResources).get(OWN.value().toLowerCase(Locale.ROOT));
        inOrder.verify(taskRoleResources).get(CLAIM.value().toLowerCase(Locale.ROOT));
        inOrder.verify(taskRoleResources).get(EXECUTE.value().toLowerCase(Locale.ROOT));
        inOrder.verify(taskRoleResources).get(ASSIGN.value().toLowerCase(Locale.ROOT));
        inOrder.verify(taskRoleResources).get(OWN.value().toLowerCase(Locale.ROOT));
        verify(builder, times(6)).isTrue(any());
        verify(builder, times(3)).and(new Predicate[]{permissionsPredicate,
            permissionsPredicate});
        verify(builder, times(2)).or(permissionsPredicate, permissionsPredicate);
    }
}
