package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;


@ExtendWith(MockitoExtension.class)
public class RoleAssignmentFilterTest {

    @Mock
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
    public void buildQueryForAllGrantTypes() {

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

        Predicate predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes(Classification.PUBLIC));
        assertNotNull(predicate);
        assertEquals(inObject, predicate);

        predicate = RoleAssignmentFilter.buildQueryForStandardAndChallenged(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes(Classification.PUBLIC));
        assertNotNull(predicate);
        assertEquals(inObject, predicate);

        predicate = RoleAssignmentFilter.buildQueryForExcluded(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes(Classification.PUBLIC));
        assertNotNull(predicate);
        assertEquals(inObject, predicate);

        predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes(Classification.PRIVATE));
        assertNotNull(predicate);
        assertEquals(inObject, predicate);

        predicate = RoleAssignmentFilter.buildQueryForBasicAndSpecific(
            root,
            taskRoleResources,
            criteriaBuilder,
            roleAssignmentWithAllGrantTypes(Classification.RESTRICTED));
        assertNotNull(predicate);
        assertEquals(inObject, predicate);
    }

    @Test
    public void buildQueryWithNullAttributesAndUnKnownGrantType() {

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
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .attributes(stdAttributesWithNull)
            .grantType(GrantType.UNKNOWN)
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

    }


    private static List<Optional<RoleAssignment>> roleAssignmentWithAllGrantTypes(Classification classification) {
        List<Optional<RoleAssignment>> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
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
            .classification(classification)
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
            .classification(classification)
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
            .classification(classification)
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
            .classification(classification)
            .attributes(excludeddAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(Optional.of(roleAssignment));

        return roleAssignments;
    }
}
