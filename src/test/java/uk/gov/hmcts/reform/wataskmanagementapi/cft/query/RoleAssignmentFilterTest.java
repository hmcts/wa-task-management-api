package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RoleAssignmentFilterTest {

/*    @Mock
    Root<TaskResource> root;

    @Mock
    Join<TaskResource, TaskRoleResource> taskRoleResources;

    @Mock
    CriteriaBuilderImpl criteriaBuilder;

    @Mock
    CriteriaBuilder.In<Object> inObject;

    @Mock
    CriteriaBuilder.In<Object> values;

    @Mock
    Path<Object> authorizations;

    @Test
    public void buildQueryForBasicAndSpecific() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentsWithGrantTypeBasic(Classification.PUBLIC)
        );

        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(booleanAssertionPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(booleanAssertionPredicate);

        RoleAssignmentFilter.buildRoleAssignmentConstraints(permissionsRequired, )
        Predicate predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes());
        assertNotNull(predicate);
        assertEquals(inObject, predicate);
    }

    @Test
    public void buildQueryForStandardAndChallenged() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(booleanAssertionPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(booleanAssertionPredicate);

        Predicate predicate = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes());
        assertNotNull(predicate);
        assertEquals(inObject, predicate);
    }

    @Test
    public void buildQueryForExcluded() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(booleanAssertionPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(booleanAssertionPredicate);

        Predicate predicate = RoleAssignmentFilter.buildQueryForExcluded(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes());
        assertNotNull(predicate);
        assertEquals(inObject, predicate);
    }

    @Test
    public void buildQueryForNoMatchingResults() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(null);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(null);
        lenient().when(inObject.value(any())).thenReturn(null);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(null);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);
        lenient().when(authorizations.isNull()).thenReturn(null);

        Predicate predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes());
        assertNull(predicate);
    }

    @Test
    public void buildQueryWithNullAttributesAndUnKnownGrantType() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);

        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(booleanAssertionPredicate);

        final Map<String, String> stdAttributesWithNull = new HashMap<>();
        stdAttributesWithNull.put(RoleAttributeDefinition.REGION.value(), null);
        stdAttributesWithNull.put(RoleAttributeDefinition.JURISDICTION.value(), null);
        stdAttributesWithNull.put(RoleAttributeDefinition.BASE_LOCATION.value(), null);
        stdAttributesWithNull.put(RoleAttributeDefinition.CASE_TYPE.value(), null);
        stdAttributesWithNull.put(RoleAttributeDefinition.CASE_ID.value(), null);

        List<Optional<RoleAssignment>> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker-1")
            .attributes(stdAttributesWithNull)
            .authorisations(Collections.emptyList())
            .grantType(GrantType.UNKNOWN)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker-2")
            .classification(Classification.PUBLIC)
            .attributes(null)
            .authorisations(null)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        Predicate predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignments);
        assertNotNull(predicate);
        assertEquals(inObject, predicate);

        predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            Collections.emptyList());
        assertNotNull(predicate);
        assertEquals(inObject, predicate);
    }

    private static List<Optional<RoleAssignment>> roleAssignmentWithAllGrantTypes() {
        List<Optional<RoleAssignment>> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PRIVATE)
            .attributes(specificAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .attributes(stdAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        final Map<String, String> excludeddAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(excludeddAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        return roleAssignments;
    }*/
}
