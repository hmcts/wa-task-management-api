package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.Builder;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

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
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.USER;
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
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);
    }

    @ParameterizedTest
    @MethodSource({
        "searchParameterForTaskQuery"
    })
    void shouldBuildTaskQuery(SearchTaskRequestScenario scenario) {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantType(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        final Specification<TaskResource> spec = TaskResourceSpecification.buildTaskQuery(
            scenario.searchTaskRequest, accessControlResponse, permissionsRequired
        );

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
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
            roleAssignmentWithSpecificGrantType(PUBLIC)
        );
        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        Specification<TaskResource> spec = TaskResourceSpecification.buildQueryForCompletable(
            scenario.searchEventAndCase, accessControlResponse, permissionsRequired, List.of("taskType"));

        Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);

        spec = TaskResourceSpecification.buildQueryForCompletable(
            scenario.searchEventAndCase, accessControlResponse, permissionsRequired, emptyList());

        predicate = spec.toPredicate(root, query, criteriaBuilder);
        assertNotNull(spec);
        assertNotNull(predicate);
    }

    private static Stream<SearchTaskRequestScenario> searchParameterForTaskQuery() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        final SearchTaskRequestScenario jurisdiction =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(STATE, SearchOperator.IN, singletonList("ASSIGNED"))
        ));
        final SearchTaskRequestScenario state =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("location"))
        ));
        final SearchTaskRequestScenario location =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(CASE_ID, SearchOperator.IN, singletonList("caseId"))
        ));
        final SearchTaskRequestScenario caseId =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(USER, SearchOperator.IN, singletonList("testUser"))
        ));
        final SearchTaskRequestScenario user =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("routine_work"))
        ));
        final SearchTaskRequestScenario workType =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("ASSIGNED")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("location")),
            new SearchParameter(CASE_ID, SearchOperator.IN, singletonList("caseId")),
            new SearchParameter(USER, SearchOperator.IN, singletonList("testUser")),
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("routine_work"))
        ));

        final SearchTaskRequestScenario allParameters =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest).build();

        return Stream.of(jurisdiction, state, location, caseId, user, workType, allParameters);
    }

    private static Stream<SearchTaskRequestScenario> searchParameterForCompletable() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            null, "eventId", "IA", "caseType");
        SearchTaskRequestScenario withoutCaseId =
            SearchTaskRequestScenario.builder().searchEventAndCase(searchEventAndCase).build();

        searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "IA", "caseType");

        SearchTaskRequestScenario withCaseId =
            SearchTaskRequestScenario.builder().searchEventAndCase(searchEventAndCase).build();

        return Stream.of(withoutCaseId, withCaseId);
    }

    @Builder
    private static class SearchTaskRequestScenario {
        SearchTaskRequest searchTaskRequest;
        SearchEventAndCase searchEventAndCase;
        List<String> taskTypes;
    }
}