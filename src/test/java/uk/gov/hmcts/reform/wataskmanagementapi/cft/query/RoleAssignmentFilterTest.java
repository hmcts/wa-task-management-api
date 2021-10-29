package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PRIVATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.RESTRICTED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.inActiveRoles;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithChallengedGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithChallengedGrantTypeAndNoAuthorizations;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithStandardGrantType;


@ExtendWith(MockitoExtension.class)
public class RoleAssignmentFilterTest {

    @Mock(extraInterfaces = Serializable.class) Root<TaskResource> root;
    @Mock(extraInterfaces = Serializable.class) CriteriaQuery<?> query;
    @Mock(extraInterfaces = Serializable.class) CriteriaBuilderImpl criteriaBuilder;
    @Mock Join<Object, Object> taskRoleResources;
    @Mock
    CriteriaBuilder.In<Object> inObject;
    @Mock
    CriteriaBuilder.In<Object> values;
    @Mock
    Path<Object> authorizations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
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
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
    }

    @Test
    void buildQueryForBasicAndSpecific() {

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantType(PUBLIC)
        );

        // Classification as public
        Specification<TaskResource> spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        // Classification as PRIVATE
        accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantType(PRIVATE)
        );
        spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        // Classification as RESTRICTED
        accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantType(RESTRICTED)
        );
        spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    void buildQueryForStandardAndExcluded() {

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

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithStandardGrantType()
        );

        Specification<TaskResource> spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    void buildQueryForChallengedAndExcluded() {

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

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithChallengedGrantType()
        );

        Specification<TaskResource> spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithChallengedGrantTypeAndNoAuthorizations()
        );

        spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    void shouldFilterWhenRoleIsNotActive() {
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            inActiveRoles()
        );

        // Classification as public
        Specification<TaskResource> spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    void shouldFilterWhenRoleBeginAndEndTimeAreNotGiven() {
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.BASIC)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );

        // Classification as public
        Specification<TaskResource> spec = RoleAssignmentFilter.buildRoleAssignmentConstraints(
            permissionsRequired, accessControlResponse);
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

}
