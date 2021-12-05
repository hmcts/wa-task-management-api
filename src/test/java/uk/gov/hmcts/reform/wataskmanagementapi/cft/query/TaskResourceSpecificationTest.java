package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.Builder;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithBasicGrantTypeOnly;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.*;

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
    @Mock Predicate mockPredicate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any(), any(), any(), any(), any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(mockPredicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(mockPredicate);
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);
    }

    @ParameterizedTest
    @MethodSource({
        "searchParameterForTaskQuery"
    })
    void shouldBuildTaskQueryWithSingleParameter(SearchTaskRequestScenario scenario) {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        final Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            scenario.searchTaskRequest, accessControlResponse, permissionsRequired
        );

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(2)).in(any());
        verify(criteriaBuilder, times(5)).conjunction();
    }

    @Test
    void shouldBuildTaskQueryWithOutSearchParameters() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(emptyList());
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest, accessControlResponse, permissionsRequired
        );

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).in(any());
        verify(criteriaBuilder, times(6)).conjunction();
    }

    @Test
    void shouldBuildTaskQueryWithNullConditions() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList(null))
        ));
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest, accessControlResponse, permissionsRequired
        );

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).in(any());
        verify(criteriaBuilder, times(6)).conjunction();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, emptyList())
        ));

        spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest, accessControlResponse, permissionsRequired
        );

        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        spec = TaskResourceSpecification.buildTaskQuery(
            null, accessControlResponse, permissionsRequired
        );

        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }


    @Test
    void shouldBuildTaskQueryWithOutSearchParametersAndReturnConjunctionAsNull() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(emptyList());
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        lenient().when(criteriaBuilder.conjunction()).thenReturn(null);

        Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest, accessControlResponse, permissionsRequired
        );

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).in(any());
        verify(criteriaBuilder, times(6)).conjunction();
    }

    @Test
    void shouldBuildTaskQueryWithAllParameters() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("caseId")),
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser")),
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        final Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            searchTaskRequest, accessControlResponse, permissionsRequired
        );
        spec.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder, times(7)).in(any());
    }

    @Test
    void shouldBuildSingleTaskQuery() {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantType(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        final Specification<TaskResource> spec = TaskResourceSpecification.buildSingleTaskQuery(
            "someTaskId", accessControlResponse, permissionsRequired);

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    @ParameterizedTest
    @MethodSource({
        "searchParameterForCompletable"
    })
    void shouldBuildSingleTaskQueryForCompletable(SearchTaskRequestScenario scenario) {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildQueryForCompletable(
            scenario.searchEventAndCase, accessControlResponse, permissionsRequired, List.of("taskType"));

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(3)).in(any());
    }

    @Test
    void shouldBuildSingleTaskQueryForCompletableNegated() {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithBasicGrantTypeOnly(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "IA", "caseType");

        Specification<TaskResource> spec = TaskResourceSpecification.buildQueryForCompletable(
            searchEventAndCase, accessControlResponse, permissionsRequired, emptyList());

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        verify(criteriaBuilder, times(2)).in(any());
    }

    private static Stream<SearchTaskRequestScenario> searchParameterForTaskQuery() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        final SearchTaskRequestScenario jurisdiction =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED"))
        ));
        final SearchTaskRequestScenario state =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));
        final SearchTaskRequestScenario availableTaskOnly =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location"))
        ));
        final SearchTaskRequestScenario location =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("caseId"))
        ));
        final SearchTaskRequestScenario caseId =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser"))
        ));
        final SearchTaskRequestScenario user =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work"))
        ));
        final SearchTaskRequestScenario workType =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        return Stream.of(jurisdiction, state, location, caseId, user, workType, availableTaskOnly);
    }

    private static Stream<SearchTaskRequestScenario> searchParameterForCompletable() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "IA", "caseType");
        SearchTaskRequestScenario withCaseId =
            SearchTaskRequestScenario.builder().searchEventAndCase(searchEventAndCase).build();

        return Stream.of(withCaseId);
    }

    @Builder
    private static class SearchTaskRequestScenario {
        SearchTaskRequest searchTaskRequest;
        SearchEventAndCase searchEventAndCase;
        List<String> taskTypes;
    }
}
