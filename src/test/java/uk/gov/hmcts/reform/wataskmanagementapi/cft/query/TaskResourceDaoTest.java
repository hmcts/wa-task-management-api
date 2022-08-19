package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@ExtendWith(MockitoExtension.class)
class TaskResourceDaoTest {
    public static final Map<String, String> ADDITIONAL_PROPERTIES = Map.of(
        "name1",
        "value1",
        "name2",
        "value2",
        "name3",
        "value3"
    );

    @Mock
    private EntityManager em;
    @Mock
    private CriteriaQuery<TaskResource> criteriaQuery;
    @Mock
    private Root<TaskResource> root;
    @Mock
    private Root<TaskResource> subRoot;
    @Mock
    private CriteriaQuery<Long> countCriteriaQuery;
    @Mock
    private Subquery<TaskResource> subQuery;
    @Mock
    private CriteriaQuery<Object[]> summaryCriteriaQuery;
    @Mock
    private TypedQuery<TaskResource> query;
    @Mock
    private TypedQuery<Long> countQuery;
    @Mock
    private TypedQuery<Object[]> summaryQuery;
    @Mock
    private Predicate predicate;
    @Mock
    private CriteriaBuilder.In<Object> inObject;
    @Mock
    private CriteriaBuilder.In<Object> values;
    @Mock
    private Expression<Long> selection;
    @Mock
    private Path<Object> authorizations;
    @Mock
    private Join<Object, Object> taskRoleResources;
    @Mock
    private Path<Object> path;
    @Mock(extraInterfaces = Serializable.class)
    private CriteriaBuilderImpl builder;

    @InjectMocks
    private TaskResourceDao taskResourceDao;

    @BeforeEach
    void beforeEach() {
        lenient().when(em.getCriteriaBuilder()).thenReturn(builder);
        lenient().when(builder.createQuery(TaskResource.class)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.distinct(true)).thenReturn(criteriaQuery);
        lenient().when(criteriaQuery.from(TaskResource.class)).thenReturn(root);
        lenient().when(builder.equal(any(), anyString())).thenReturn(predicate);
        lenient().when(em.createQuery(criteriaQuery)).thenReturn(query);
        lenient().when(query.setFirstResult(0)).thenReturn(query);
        lenient().when(query.setFirstResult(1)).thenReturn(query);
        lenient().when(query.setMaxResults(0)).thenReturn(query);
        lenient().when(query.setMaxResults(10)).thenReturn(query);
        lenient().when(builder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(builder.or(any(), any())).thenReturn(inObject);
        lenient().when(builder.or(any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any())).thenReturn(inObject);
        lenient().when(builder.and(any(), any(), any(), any(), any(), any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            builder,
            null,
            Boolean.TRUE
        );
        lenient().when(builder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(builder.equal(any(), any())).thenReturn(predicate);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(predicate);
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);

        //count criteria
        lenient().when(builder.createQuery(Long.class)).thenReturn(countCriteriaQuery);
        lenient().when(countCriteriaQuery.from(TaskResource.class)).thenReturn(root);
        lenient().when(countCriteriaQuery.subquery(TaskResource.class)).thenReturn(subQuery);
        lenient().when(subQuery.from(TaskResource.class)).thenReturn(subRoot);
        lenient().when(subRoot.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(subRoot.get(anyString())).thenReturn(path);
        lenient().when(subRoot.get(anyString()).get(anyString())).thenReturn(path);
        lenient().when(subQuery.where(predicate)).thenReturn(subQuery);
        lenient().when(subQuery.distinct(true)).thenReturn(subQuery);
        lenient().when(subQuery.select(any())).thenReturn(subQuery);
        lenient().when(em.createQuery(countCriteriaQuery)).thenReturn(countQuery);
        lenient().when(builder.count(root)).thenReturn(selection);
        lenient().when(countCriteriaQuery.select(selection)).thenReturn(countCriteriaQuery);
        lenient().when(countCriteriaQuery.distinct(true)).thenReturn(countCriteriaQuery);
        lenient().when(countQuery.setFirstResult(1)).thenReturn(countQuery);

        //summary criteria
        lenient().when(builder.createQuery(Object[].class)).thenReturn(summaryCriteriaQuery);
        lenient().when(summaryCriteriaQuery.from(TaskResource.class)).thenReturn(root);
        lenient().when(em.createQuery(summaryCriteriaQuery)).thenReturn(summaryQuery);
        lenient().when(summaryCriteriaQuery.multiselect(anyList())).thenReturn(summaryCriteriaQuery);
        lenient().when(summaryCriteriaQuery.distinct(true)).thenReturn(summaryCriteriaQuery);
        lenient().when(summaryQuery.setFirstResult(0)).thenReturn(summaryQuery);
        lenient().when(summaryQuery.setFirstResult(1)).thenReturn(summaryQuery);
        lenient().when(summaryQuery.setMaxResults(0)).thenReturn(summaryQuery);
        lenient().when(summaryQuery.setMaxResults(10)).thenReturn(summaryQuery);
    }

    private Object[] createTaskResourceSummary() {
        return new Object[]{
            "taskId",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "TestCase",
            "Asylum",
            "Taylor House",
            "title"
        };
    }

    private TaskResource createTaskResource() {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(new TaskRoleResource(
                "tribunal-caseworker",
                true,
                false,
                false,
                false,
                false,
                false,
                new String[]{"SPECIFIC", "STANDARD"},
                0,
                false,
                "JUDICIAL",
                "taskId",
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
            )),
            "caseCategory",
            ADDITIONAL_PROPERTIES,
            "next-hearing-date",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

    private static List<RoleAssignment> roleAssignmentWithAllGrantTypes() {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(Classification.PUBLIC)
            .grantType(GrantType.SPECIFIC)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(specificAttributes)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(stdAttributes)
            .roleType(RoleType.ORGANISATION)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .roleType(RoleType.CASE)
            .classification(Classification.PUBLIC)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> excludeddAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(Classification.PUBLIC)
            .attributes(excludeddAttributes)
            .grantType(GrantType.EXCLUDED)
            .roleType(RoleType.ORGANISATION)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    @Test
    void shouldReturnTaskSummary() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, List.of("hearing_work"))
            ),
            List.of(
                new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT),
                new SortingParameter(SortField.LOCATION_NAME_CAMEL_CASE, SortOrder.DESCENDANT)
            )
        );

        List<RoleAssignment> roleAssignments = roleAssignmentWithAllGrantTypes();

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        when(summaryQuery.getResultList()).thenReturn(List.<Object[]>of(createTaskResourceSummary()));

        List<Object[]> taskResourceSummary
            = taskResourceDao.getTaskResourceSummary(1, 10, searchTaskRequest, roleAssignments, permissionsRequired);

        assertNotNull(taskResourceSummary);
        assertEquals("taskId", taskResourceSummary.get(0)[0]);
    }

    @Test
    void should_return_task_summary_when_sorting_parameters_are_empty() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, List.of("hearing_work"))
            ),
            List.of()
        );

        List<RoleAssignment> roleAssignments = roleAssignmentWithAllGrantTypes();

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        when(summaryQuery.getResultList()).thenReturn(List.<Object[]>of(createTaskResourceSummary()));

        List<Object[]> taskResourceSummary
            = taskResourceDao.getTaskResourceSummary(1, 10, searchTaskRequest, roleAssignments, permissionsRequired);

        assertNotNull(taskResourceSummary);
        assertEquals("taskId", taskResourceSummary.get(0)[0]);
    }

    @Test
    void should_return_task_summary_when_sorting_parameters_are_null() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, List.of("hearing_work"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, null))
        );

        List<RoleAssignment> roleAssignments = roleAssignmentWithAllGrantTypes();

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        when(summaryQuery.getResultList()).thenReturn(List.<Object[]>of(createTaskResourceSummary()));

        List<Object[]> taskResourceSummary
            = taskResourceDao.getTaskResourceSummary(1, 10, searchTaskRequest, roleAssignments, permissionsRequired);

        assertNotNull(taskResourceSummary);
        assertEquals("taskId", taskResourceSummary.get(0)[0]);
    }

    @Test
    void shouldReturnTasks() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, List.of("hearing_work"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        when(query.getResultList()).thenReturn(List.of(createTaskResource()));

        List<TaskResource> taskResources = taskResourceDao.getTaskResources(
            searchTaskRequest,
            List.<Object[]>of(createTaskResourceSummary())
        );

        assertNotNull(taskResources);
        assertEquals("taskId", taskResources.get(0).getTaskId());
    }


    @Test
    void shouldReturnCompletableTasks() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "someCaseId",
            "someEventId",
            "IA",
            "Asylum"
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        when(query.getResultList()).thenReturn(List.of(createTaskResource()));

        List<TaskResource> taskResources = taskResourceDao.getCompletableTaskResources(
            searchEventAndCase,
            roleAssignmentWithAllGrantTypes(),
            permissionsRequired,
            List.of("reviewTheAppeal")
        );

        assertNotNull(taskResources);
        assertEquals("taskId", taskResources.get(0).getTaskId());
    }

    @Test
    void shouldReturnTotalTaskCounts() {
        final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, List.of("hearing_work"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        when(countQuery.getSingleResult()).thenReturn(1L);

        Long totalCount = taskResourceDao.getTotalCount(
            searchTaskRequest,
            roleAssignmentWithAllGrantTypes(),
            permissionsRequired
        );

        assertEquals(1, totalCount);
    }

    @Test
    void shouldGetTask() {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder().buildSingleType(READ);

        String taskId = "taskId";
        TaskResource expectedTask = new TaskResource(
            taskId,
            "takeName",
            "taskType",
            UNCONFIGURED,
            "caseId"
        );

        when(query.getResultList()).thenReturn(List.of(expectedTask));

        Optional<TaskResource> task
            = taskResourceDao.getTask(taskId, roleAssignmentWithAllGrantTypes(), permissionsRequired);

        assertThat(task)
            .isPresent()
            .get().isEqualTo(expectedTask);
    }


    @Test
    void should_raise_exception_when_invalid_limit() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003"))
            ),
            List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT))
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        assertThatThrownBy(() -> taskResourceDao.getTaskResourceSummary(
            0,
            0,
            searchTaskRequest,
            roleAssignmentWithAllGrantTypes(),
            permissionsRequired
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Limit must not be less than one");
    }

    @Test
    void should_raise_exception_when_invalid_offset() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, List.of("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, List.of("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, List.of("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, List.of("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, List.of("1623278362431003"))
            ),
            List.of()
        );

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(PermissionTypes.READ);

        assertThatThrownBy(() -> taskResourceDao.getTaskResourceSummary(
            -1,
            25,
            searchTaskRequest,
            roleAssignmentWithAllGrantTypes(),
            permissionsRequired
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Offset index must not be less than zero");
    }

}
