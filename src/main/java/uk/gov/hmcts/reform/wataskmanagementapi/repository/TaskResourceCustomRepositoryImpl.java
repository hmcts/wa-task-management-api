package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.SqlResultSetMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskSearchSortProvider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TaskResourceCustomRepositoryImpl implements TaskResourceCustomRepository {
    private static final String BASE_QUERY =
        "%sFROM {h-schema}tasks t "
        + "WHERE indexed "
        + "AND {h-schema}filter_signatures(t.task_id, t.state, t.jurisdiction, t.role_category, t.work_type, t.region, "
        + "t.location) && CAST(:filterSignature AS text[]) "
        + "AND {h-schema}role_signatures(t.task_id, t.jurisdiction, t.region, t.location, t.case_id, "
        + "t.security_classification) && CAST(:roleSignature AS text[]) "
        + "%s%s%s";

    private static final String SELECT_CLAUSE = "SELECT t.task_id ";

    private static final String DB_COL_ASSIGNEE = "assignee";
    private static final String COUNT_CLAUSE = "SELECT count(*) ";
    private static final String PAGINATION_CLAUSE = "OFFSET :firstResult LIMIT :maxResults";

    protected static final String RESULT_MAPPER = "TaskSearchResult";
    private static final int ONE = 1;

    @PersistenceContext
    private EntityManager entityManager;

    @SqlResultSetMapping(name = RESULT_MAPPER,
        columns = {
            @ColumnResult(name = "task_id", type = String.class)
        }
    )
    @Entity
    class TaskSearchResult {
        @Id
        int id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchTasksIds(int firstResult,
                                       int maxResults,
                                       Set<String> filterSignature,
                                       Set<String> roleSignature,
                                       List<String> excludeCaseIds,
                                       SearchRequest searchRequest) {

        String queryString = String.format(BASE_QUERY,
            SELECT_CLAUSE,
            extraConstraints(excludeCaseIds, searchRequest),
            TaskSearchSortProvider.getSortOrderQuery(searchRequest),
            PAGINATION_CLAUSE
        );

        log.info("Task search query [{}]", queryString);
        Query query = entityManager.createNativeQuery(queryString, RESULT_MAPPER);
        addParameters(query, firstResult, maxResults, filterSignature, roleSignature, excludeCaseIds, searchRequest);

        List<String> taskIds = query.getResultList();
        log.info("Number of tasks returned {}", CollectionUtils.isEmpty(taskIds) ? 0 : taskIds.size());

        return taskIds;
    }

    @Override
    public Long searchTasksCount(Set<String> filterSignature,
                                 Set<String> roleSignature,
                                 List<String> excludeCaseIds,
                                 SearchRequest searchRequest) {

        String queryString = String.format(BASE_QUERY,
            COUNT_CLAUSE,
            extraConstraints(excludeCaseIds, searchRequest),
            "", "");

        log.info("Task count query [{}]", queryString);
        Query query = entityManager.createNativeQuery(queryString);
        addParameters(query, filterSignature, roleSignature, excludeCaseIds, searchRequest);

        Long taskCount = ((Number) query.getSingleResult()).longValue();
        log.info("Total number of tasks {}", taskCount);

        return taskCount;
    }

    void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    private String extraConstraints(List<String> excludeCaseIds, SearchRequest searchRequest) {
        StringBuilder extraConstraints = new StringBuilder("");
        if (searchRequest.isAvailableTasksOnly()) {
            extraConstraints.append("AND assignee IS NULL ");
        } else {
            extraConstraints.append(buildListConstraint(searchRequest.getUsers(),
                                                        DB_COL_ASSIGNEE, DB_COL_ASSIGNEE, true));
        }
        if (CollectionUtils.isEmpty(searchRequest.getCftTaskStates())) {
            extraConstraints.append("AND state IN ('ASSIGNED', 'UNASSIGNED') ");
        } else {
            String states = searchRequest.getCftTaskStates()
                .stream()
                .map(s -> "'" + s.getValue() + "'")
                .collect(Collectors.joining(", "));
            extraConstraints.append("AND state IN (").append(states).append(") ");
        }
        extraConstraints.append(buildListConstraint(searchRequest.getCaseIds(), "case_id", "caseId", true))
            .append(buildListConstraint(excludeCaseIds, "case_id", "excludedCaseId", false))
            .append(buildListConstraint(searchRequest.getTaskTypes(), "task_type", "taskType", true));
        return extraConstraints.toString();
    }

    private String buildListConstraint(List<?> values, String column, String paramName, boolean include) {
        if (!CollectionUtils.isEmpty(values)) {
            if (values.size() == ONE) {
                return "AND " + column + (include ? " = " : " <> ") + ":" + paramName + " ";
            } else {
                return "AND " + column + (include ? " " : " NOT ") + "IN (:" + paramName + ") ";
            }
        }
        return "";
    }

    private void addParameters(Query query,
                               int firstResult,
                               int maxResults,
                               Set<String> filterSignature,
                               Set<String> roleSignature,
                               List<String> excludeCaseIds,
                               SearchRequest searchRequest) {

        addParameters(query, filterSignature, roleSignature, excludeCaseIds, searchRequest);
        query.setParameter("firstResult", firstResult);
        query.setParameter("maxResults", maxResults);
    }

    private void addParameters(Query query,
                               Set<String> filterSignature,
                               Set<String> roleSignature,
                               List<String> excludeCaseIds,
                               SearchRequest searchRequest) {

        query.setParameter("filterSignature", filterSignature.toArray(new String[0]));
        query.setParameter("roleSignature", roleSignature.toArray(new String[0]));
        List<String> users = searchRequest.getUsers();
        if (!searchRequest.isAvailableTasksOnly() && !CollectionUtils.isEmpty(users)) {
            setParameter(query, DB_COL_ASSIGNEE, users);
        }
        List<String> caseIds = searchRequest.getCaseIds();
        if (!CollectionUtils.isEmpty(searchRequest.getCaseIds())) {
            setParameter(query, "caseId", caseIds);
        }
        List<String> taskTypes = searchRequest.getTaskTypes();
        if (!CollectionUtils.isEmpty(searchRequest.getTaskTypes())) {
            setParameter(query, "taskType", taskTypes);
        }
        if (!CollectionUtils.isEmpty(excludeCaseIds)) {
            setParameter(query, "excludedCaseId", excludeCaseIds);
        }
    }

    private void setParameter(Query query, String name, List<String> values) {
        query.setParameter(name, values.size() == 1 ? values.get(0) : values);
    }
}
