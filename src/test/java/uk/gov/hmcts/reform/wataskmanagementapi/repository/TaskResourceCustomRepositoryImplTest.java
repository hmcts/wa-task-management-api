package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceCustomRepositoryImpl.RESULT_MAPPER;

@ExtendWith(MockitoExtension.class)
class TaskResourceCustomRepositoryImplTest {
    private static final String ROLE_CRITERIA_CTE =
        "WITH request_role_criteria("
            + "jurisdiction, region, location, role_name, case_id, permission, classification, authorization_value"
            + ") AS (VALUES ("
            + "CAST(:roleJurisdiction0 AS text), "
            + "CAST(NULL AS text), "
            + "CAST(NULL AS text), "
            + "CAST(:roleName0 AS text), "
            + "CAST(NULL AS text), "
            + "CAST(:rolePermission0 AS text), "
            + "CAST(:roleClassification0 AS text), "
            + "CAST(NULL AS text))) ";
    private static final String ROLE_CRITERIA_CONSTRAINTS =
        "AND EXISTS ("
            + "SELECT 1 FROM {h-schema}task_search_permissions tsp "
            + "JOIN request_role_criteria role_criteria "
            + "ON role_criteria.role_name = tsp.role_name "
            + "AND role_criteria.permission = tsp.permission "
            + "WHERE tsp.task_id = t.task_id "
            + "AND (role_criteria.jurisdiction IS NULL OR role_criteria.jurisdiction = t.jurisdiction) "
            + "AND (role_criteria.region IS NULL OR role_criteria.region = t.region) "
            + "AND (role_criteria.location IS NULL OR role_criteria.location = t.location) "
            + "AND (role_criteria.case_id IS NULL OR role_criteria.case_id = t.case_id) "
            + "AND (role_criteria.case_id IS NOT NULL "
            + "OR tsp.authorization_value IS NOT DISTINCT FROM role_criteria.authorization_value) "
            + "AND ("
            + "(t.security_classification::text = 'PUBLIC' AND role_criteria.classification IN ('U', 'P', 'R')) "
            + "OR (t.security_classification::text = 'PRIVATE' AND role_criteria.classification IN ('P', 'R')) "
            + "OR (t.security_classification::text = 'RESTRICTED' AND role_criteria.classification = 'R')"
            + ")"
            + ") ";
    private static final String OLD_SIGNATURE_CONSTRAINTS =
        "AND {h-schema}filter_signatures(t.task_id, t.state, t.jurisdiction, t.role_category, t.work_type, "
        + "t.region, t.location) && CAST(:filterSignature AS text[]) "
        + "AND {h-schema}role_signatures(t.task_id, t.jurisdiction, t.region, t.location, t.case_id, "
        + "t.security_classification) && CAST(:roleSignature AS text[]) ";

    @Mock
    EntityManager entityManager;

    @Mock
    Query query;

    Set<String> filterSignature = Set.of("*:IA:*:*:1:765324");
    Set<String> roleSignature = Set.of("IA:*:*:tribunal-caseofficer:*:r:U:*");
    List<TaskSearchRoleCriteria> roleCriteria = List.of(
        new TaskSearchRoleCriteria("IA", null, null, "tribunal-caseofficer", null, "r", "U", null)
    );


    TaskResourceCustomRepositoryImpl taskResourceCustomRepository;


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
    void when_search_request_is_empty_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            null, SearchRequest.builder().build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);
    }

    @Test
    void when_search_request_with_order_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            null, SearchRequest.builder()
                    .sortingParameters(List.of(new SortingParameter(SortField.CASE_ID, SortOrder.ASCENDANT),
                        new SortingParameter(SortField.CASE_NAME_CAMEL_CASE, SortOrder.ASCENDANT)))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') "
                       + "ORDER BY case_id ASC, case_name ASC, "
                          + "major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);
    }

    @Test
    void when_search_request_is_empty_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, null,
            SearchRequest.builder().build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
    }

    @Test
    void when_search_request_is_empty_then_build_old_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCountOld(filterSignature, roleSignature,null,
            SearchRequest.builder().build());

        String queryStr = "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + OLD_SIGNATURE_CONSTRAINTS
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        inOrder.verify(query).setParameter("filterSignature", new String[]{"*:IA:*:*:1:765324"});
        inOrder.verify(query).setParameter("roleSignature", new String[]{"IA:*:*:tribunal-caseofficer:*:r:U:*"});
    }

    @Test
    void when_search_request_for_available_task_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            null, SearchRequest.builder()
                .requestContext(RequestContext.AVAILABLE_TASKS)
                .users(List.of("user"))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IS NULL "
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);
    }

    @Test
    void when_search_request_for_available_task_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, null,
            SearchRequest.builder()
            .requestContext(RequestContext.AVAILABLE_TASKS)
            .users(List.of("user"))
            .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IS NULL "
                       + "AND state IN ('ASSIGNED', 'UNASSIGNED') ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
    }

    @Test
    void when_search_with_single_search_filter_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            null, SearchRequest.builder()
                .users(List.of("user"))
                    .cftTaskStates(List.of(CFTTaskState.COMPLETED))
                    .caseIds(List.of("caseId"))
                    .taskTypes(List.of("TaskType"))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee = :assignee "
                       + "AND state IN ('COMPLETED') "
                       + "AND case_id = :caseId "
                       + "AND task_type = :taskType "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", "user");
        inOrder.verify(query).setParameter("caseId", "caseId");
        inOrder.verify(query).setParameter("taskType", "TaskType");
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);

    }

    @Test
    void when_search_with_single_search_filter_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, null,
            SearchRequest.builder()
            .users(List.of("user"))
            .cftTaskStates(List.of(CFTTaskState.COMPLETED))
            .caseIds(List.of("caseId"))
            .taskTypes(List.of("TaskType"))
            .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee = :assignee "
                       + "AND state IN ('COMPLETED') "
                       + "AND case_id = :caseId "
                       + "AND task_type = :taskType ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", "user");
        inOrder.verify(query).setParameter("caseId", "caseId");
        inOrder.verify(query).setParameter("taskType", "TaskType");
    }

    @Test
    void when_search_with_multiple_search_filter_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            null, SearchRequest.builder()
                .users(List.of("user", "user2"))
                .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
                .caseIds(List.of("caseId", "caseId2"))
                .taskTypes(List.of("TaskType", "TaskType2"))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND task_type IN (:taskType) "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);

    }

    @Test
    void when_search_with_multiple_search_filter_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, null,
            SearchRequest.builder()
            .users(List.of("user", "user2"))
            .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
            .caseIds(List.of("caseId", "caseId2"))
            .taskTypes(List.of("TaskType", "TaskType2"))
            .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND task_type IN (:taskType) ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
    }

    @Test
    void when_search_with_multiple_search_filter_and_excluded_case_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            List.of("caseId"), SearchRequest.builder()
                .users(List.of("user", "user2"))
                .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
                .caseIds(List.of("caseId", "caseId2"))
                .taskTypes(List.of("TaskType", "TaskType2"))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND case_id <> :excludedCaseId "
                       + "AND task_type IN (:taskType) "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
        inOrder.verify(query).setParameter("excludedCaseId", "caseId");
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);

    }

    @Test
    void when_search_with_multiple_search_filter_and_excluded_case_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, List.of("caseId"),
            SearchRequest.builder()
            .users(List.of("user", "user2"))
            .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
            .caseIds(List.of("caseId", "caseId2"))
            .taskTypes(List.of("TaskType", "TaskType2"))
            .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND case_id <> :excludedCaseId "
                       + "AND task_type IN (:taskType) ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
        inOrder.verify(query).setParameter("excludedCaseId", "caseId");
    }

    @Test
    void when_search_with_multiple_search_filter_and_multiple_excluded_case_then_build_search_query_with_signatures() {
        taskResourceCustomRepository.searchTasksIds(1, 25, roleCriteria,
            List.of("caseId", "caseId2"), SearchRequest.builder()
                .users(List.of("user", "user2"))
                .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
                .caseIds(List.of("caseId", "caseId2"))
                .taskTypes(List.of("TaskType", "TaskType2"))
                .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT t.task_id FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND case_id NOT IN (:excludedCaseId) "
                       + "AND task_type IN (:taskType) "
                       + "ORDER BY major_priority ASC, priority_date ASC, minor_priority ASC, task_id ASC "
                       + "OFFSET :firstResult LIMIT :maxResults";
        verify(entityManager).createNativeQuery(queryStr, RESULT_MAPPER);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
        inOrder.verify(query).setParameter("excludedCaseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("firstResult", 1);
        inOrder.verify(query).setParameter("maxResults", 25);

    }

    @Test
    void when_search_with_multiple_search_filter_and_multiple_excluded_case_then_build_count_query_with_signatures() {
        taskResourceCustomRepository.searchTasksCount(roleCriteria, List.of("caseId", "caseId2"),
            SearchRequest.builder()
            .users(List.of("user", "user2"))
            .cftTaskStates(List.of(CFTTaskState.COMPLETED, CFTTaskState.CONFIGURED))
            .caseIds(List.of("caseId", "caseId2"))
            .taskTypes(List.of("TaskType", "TaskType2"))
            .build());

        String queryStr = ROLE_CRITERIA_CTE + "SELECT count(*) FROM {h-schema}tasks t WHERE indexed "
                       + ROLE_CRITERIA_CONSTRAINTS
                       + "AND assignee IN (:assignee) "
                       + "AND state IN ('COMPLETED', 'CONFIGURED') "
                       + "AND case_id IN (:caseId) "
                       + "AND case_id NOT IN (:excludedCaseId) "
                       + "AND task_type IN (:taskType) ";
        verify(entityManager).createNativeQuery(queryStr);
        InOrder inOrder = inOrder(query);
        verifyNewSignatureParameters(inOrder);
        inOrder.verify(query).setParameter("assignee", List.of("user", "user2"));
        inOrder.verify(query).setParameter("caseId", List.of("caseId", "caseId2"));
        inOrder.verify(query).setParameter("taskType", List.of("TaskType", "TaskType2"));
        inOrder.verify(query).setParameter("excludedCaseId", List.of("caseId", "caseId2"));
    }

    private void verifyNewSignatureParameters(InOrder inOrder) {
        inOrder.verify(query).setParameter("roleJurisdiction0", "IA");
        inOrder.verify(query).setParameter("roleName0", "tribunal-caseofficer");
        inOrder.verify(query).setParameter("rolePermission0", "r");
        inOrder.verify(query).setParameter("roleClassification0", "U");
    }
}
