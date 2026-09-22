package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceCustomRepositoryImpl.RESULT_MAPPER;

@ExtendWith(MockitoExtension.class)
class TaskResourceCustomRepositoryImplTest {

    private static final String OLD_SIGNATURE_CONSTRAINTS =
        "AND {h-schema}filter_signatures(t.task_id, t.state, t.jurisdiction, t.role_category, t.work_type, "
        + "t.region, t.location) && CAST(:filterSignature AS text[]) "
        + "AND {h-schema}role_signatures(t.task_id, t.jurisdiction, t.region, t.location, t.case_id, "
        + "t.security_classification) && CAST(:roleSignature AS text[]) ";

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private final Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
    private final Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*");
    private final List<TaskSearchRoleCriteria> roleCriteria = List.of(
        new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseofficer", null, "r", "U", null)
    );

    private TaskResourceCustomRepositoryImpl taskResourceCustomRepository;

    @BeforeEach
    void setUp() {
        taskResourceCustomRepository = new TaskResourceCustomRepositoryImpl();
        taskResourceCustomRepository.setEntityManager(entityManager);
        lenient().when(entityManager.createNativeQuery(anyString(), eq(RESULT_MAPPER))).thenReturn(query);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.getResultList()).thenReturn(List.of());
        lenient().when(query.getSingleResult()).thenReturn(BigInteger.ONE);
    }

    @Test
    void should_build_legacy_search_index_page_query() {
        SearchRequest searchRequest = SearchRequest.builder().build();

        taskResourceCustomRepository.searchTasksIdsUsingSearchIndex(
            1, 25, filterSignature, roleSignature, List.of(), searchRequest);

        String queryString = "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                             + OLD_SIGNATURE_CONSTRAINTS
                             + "AND state IN ('ASSIGNED', 'UNASSIGNED') "
                             + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                             + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryString, RESULT_MAPPER);
        verify(query).setParameter("filterSignature", filterSignature.toArray(new String[0]));
        verify(query).setParameter("roleSignature", roleSignature.toArray(new String[0]));
        verify(query).setParameter("firstResult", 1);
        verify(query).setParameter("maxResults", 25);
    }

    @Test
    void should_build_legacy_search_index_count_query() {
        SearchRequest searchRequest = SearchRequest.builder().build();

        Long count = taskResourceCustomRepository.searchTasksCountUsingSearchIndex(
            filterSignature, roleSignature, List.of(), searchRequest);

        String queryString = "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                             + OLD_SIGNATURE_CONSTRAINTS
                             + "AND state IN ('ASSIGNED', 'UNASSIGNED') ";
        verify(entityManager).createNativeQuery(queryString);
        verify(query).setParameter("filterSignature", filterSignature.toArray(new String[0]));
        verify(query).setParameter("roleSignature", roleSignature.toArray(new String[0]));
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void should_build_task_role_page_query_and_bind_pagination() {
        taskResourceCustomRepository.searchTasksIdsUsingTaskRoles(
            25, 10, roleCriteria, List.of("excluded-case"), SearchRequest.builder().build());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture(), eq(RESULT_MAPPER));
        assertThat(sql.getValue())
            .startsWith("SELECT t.task_id")
            .contains("FROM {h-schema}tasks t", "EXISTS (", "FROM {h-schema}task_roles tr")
            .contains("OFFSET 0", "t.case_id <> :excludedCaseId")
            .containsOnlyOnce("t.state IN ('ASSIGNED', 'UNASSIGNED')")
            .endsWith("OFFSET :firstResult LIMIT :maxResults");
        verify(query).setParameter("scope_r_0_jurisdiction", "IA");
        verify(query).setParameter("scope_r_0_roleNames", List.of("tribunal-caseofficer"));
        verify(query).setParameter("excludedCaseId", "excluded-case");
        verify(query).setParameter("firstResult", 25);
        verify(query).setParameter("maxResults", 10);
    }

    @Test
    void should_apply_search_filters_and_sorting_to_task_role_page_query() {
        SearchRequest searchRequest = SearchRequest.builder()
            .users(List.of("user-1", "user-2"))
            .caseIds(List.of("case-1", "case-2"))
            .taskTypes(List.of("task-type"))
            .jurisdictions(List.of("IA"))
            .locations(List.of("765324"))
            .regions(List.of("1"))
            .workTypes(List.of("decision-making-work"))
            .sortingParameters(List.of(new SortingParameter(SortField.CASE_ID, SortOrder.DESCENDANT)))
            .build();

        taskResourceCustomRepository.searchTasksIdsUsingTaskRoles(
            0, 25, roleCriteria, List.of("excluded-1", "excluded-2"), searchRequest);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture(), eq(RESULT_MAPPER));
        assertThat(sql.getValue())
            .contains("t.assignee IN (:assignee)")
            .contains("t.case_id IN (:caseId)")
            .contains("t.case_id NOT IN (:excludedCaseId)")
            .contains("t.task_type = :taskType")
            .contains("t.jurisdiction = :jurisdiction")
            .contains("t.location = :location")
            .contains("t.region = :region")
            .contains("t.work_type = :workType")
            .contains("ORDER BY case_id DESC");
        verify(query).setParameter("assignee", List.of("user-1", "user-2"));
        verify(query).setParameter("caseId", List.of("case-1", "case-2"));
        verify(query).setParameter("excludedCaseId", List.of("excluded-1", "excluded-2"));
        verify(query).setParameter("taskType", "task-type");
        verify(query).setParameter("jurisdiction", "IA");
        verify(query).setParameter("location", "765324");
        verify(query).setParameter("region", "1");
        verify(query).setParameter("workType", "decision-making-work");
    }

    @Test
    void should_preserve_available_task_constraints_in_page_and_count_queries() {
        SearchRequest searchRequest = SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .users(List.of("ignored-user"))
            .build();

        taskResourceCustomRepository.searchTasksIdsUsingTaskRoles(
            0, 25, roleCriteria, List.of(), searchRequest);
        taskResourceCustomRepository.searchTasksCountUsingTaskRoles(roleCriteria, List.of(), searchRequest);

        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(pageSql.capture(), eq(RESULT_MAPPER));
        verify(entityManager).createNativeQuery(countSql.capture());
        assertThat(List.of(pageSql.getValue(), countSql.getValue())).allSatisfy(sql ->
            assertThat(sql)
                .contains("t.assignee IS NULL")
                .containsOnlyOnce("t.state IN ('ASSIGNED', 'UNASSIGNED')")
                .doesNotContain(":assignee"));
    }

    @Test
    void should_build_uncapped_task_role_count_query() {
        Long count = taskResourceCustomRepository.searchTasksCountUsingTaskRoles(
            roleCriteria, List.of("excluded-case"), SearchRequest.builder().build());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        assertThat(sql.getValue())
            .startsWith("SELECT count(*)")
            .contains("EXISTS (", "t.case_id <> :excludedCaseId")
            .containsOnlyOnce("t.state IN ('ASSIGNED', 'UNASSIGNED')")
            .doesNotContain("OFFSET", "LIMIT", "ORDER BY", "task_search_permissions");
        verify(query).setParameter("scope_r_0_jurisdiction", "IA");
        verify(query).setParameter("scope_r_0_roleNames", List.of("tribunal-caseofficer"));
        verify(query).setParameter("excludedCaseId", "excluded-case");
        assertThat(count).isEqualTo(1L);
    }

    @ParameterizedTest(name = "requested states {0} produce {1}")
    @MethodSource("taskRoleStates")
    void should_normalize_active_states_once_for_task_role_page_and_count(List<CFTTaskState> states,
                                                                          String expectedConstraint) {
        SearchRequest searchRequest = SearchRequest.builder().cftTaskStates(states).build();

        taskResourceCustomRepository.searchTasksIdsUsingTaskRoles(0, 25, roleCriteria, List.of(), searchRequest);
        taskResourceCustomRepository.searchTasksCountUsingTaskRoles(roleCriteria, List.of(), searchRequest);

        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(pageSql.capture(), eq(RESULT_MAPPER));
        verify(entityManager).createNativeQuery(countSql.capture());
        assertThat(List.of(pageSql.getValue(), countSql.getValue())).allSatisfy(sql -> {
            assertThat(sql).containsOnlyOnce(expectedConstraint).doesNotContain("COMPLETED", "CANCELLED");
            if ("AND FALSE".equals(expectedConstraint)) {
                assertThat(sql).doesNotContain("t.state");
            } else {
                assertThat(sql).containsOnlyOnce("t.state").doesNotContain("AND FALSE");
            }
        });
    }

    @Test
    void should_preserve_requested_legacy_states_without_intersecting_active_states() {
        SearchRequest searchRequest = SearchRequest.builder()
            .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CANCELLED))
            .build();

        taskResourceCustomRepository.searchTasksIdsUsingSearchIndex(
            0, 25, filterSignature, roleSignature, List.of(), searchRequest);
        taskResourceCustomRepository.searchTasksCountUsingSearchIndex(
            filterSignature, roleSignature, List.of(), searchRequest);

        ArgumentCaptor<String> pageSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(pageSql.capture(), eq(RESULT_MAPPER));
        verify(entityManager).createNativeQuery(countSql.capture());
        assertThat(List.of(pageSql.getValue(), countSql.getValue())).allSatisfy(sql ->
            assertThat(sql)
                .containsOnlyOnce("AND state IN ('COMPLETED', 'CANCELLED')")
                .doesNotContain("AND FALSE", "state IN ('ASSIGNED', 'UNASSIGNED')"));
    }

    private static Stream<Arguments> taskRoleStates() {
        return Stream.of(
            Arguments.of(null, "AND t.state IN ('ASSIGNED', 'UNASSIGNED')"),
            Arguments.of(List.of(), "AND t.state IN ('ASSIGNED', 'UNASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.ASSIGNED), "AND t.state IN ('ASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.UNASSIGNED), "AND t.state IN ('UNASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED),
                "AND t.state IN ('ASSIGNED', 'UNASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.UNASSIGNED, CFTTaskState.UNASSIGNED),
                "AND t.state IN ('UNASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.COMPLETED, CFTTaskState.CANCELLED), "AND FALSE"),
            Arguments.of(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED), "AND t.state IN ('ASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.COMPLETED, CFTTaskState.UNASSIGNED), "AND t.state IN ('UNASSIGNED')"),
            Arguments.of(List.of(CFTTaskState.ASSIGNED, CFTTaskState.COMPLETED, CFTTaskState.UNASSIGNED,
                CFTTaskState.ASSIGNED), "AND t.state IN ('ASSIGNED', 'UNASSIGNED')")
        );
    }
}
