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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.WORK_TYPE;

@ExtendWith(MockitoExtension.class)
public class TaskResourceSpecificationTest {

    @Mock(extraInterfaces = Serializable.class) Root<TaskResource> root;
    @Mock(extraInterfaces = Serializable.class) CriteriaQuery<?> query;
    @Mock(extraInterfaces = Serializable.class) CriteriaBuilderImpl criteriaBuilder;
    @Mock Join<Object, Object> taskRoleResources;
    @Mock CriteriaBuilder.In<Object> inObject;
    @Mock CriteriaBuilder.In<Object> values;
    @Mock Path<Object> authorizations;
    @Mock Path<Object> path;

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

        BooleanAssertionPredicate taskIdPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);
    }

    @Test
    public void buildQueryForBasicGrantTypes() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentsWithGrantTypeBasic(Classification.PUBLIC)
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired);

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    public void buildQueryForNullSearchTask() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            null,
            accessControlResponse,
            permissionsRequired
        );

        lenient().when(criteriaBuilder.conjunction()).thenReturn(null);

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    public void buildQueryForNullSearchParameter() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(null);
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired
        );
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }


    @Test
    public void buildQueryForInactiveRole() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("inActiveRole")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired
        );
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    public void buildQueryForBeginTimeAfterEndTime() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().plusYears(1))
            .endTime(LocalDateTime.now().minusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired
        );
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    public void buildQueryForWorkType() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("routine_work"))
        ));

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentsWithGrantTypeBasic(Classification.PUBLIC)
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired);

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @Test
    public void buildQueryForInvalidBeginAndEndTime() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, asList("IA"))
        ));

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("tribunal-caseworker")
            .classification(Classification.RESTRICTED)
            .grantType(GrantType.BASIC)
            .beginTime(null)
            .endTime(null)
            .build();
        roleAssignments.add(roleAssignment);

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignments
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired
        );
        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }


    private static List<RoleAssignment> roleAssignmentsWithGrantTypeBasic(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("other-caseworker")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

}
