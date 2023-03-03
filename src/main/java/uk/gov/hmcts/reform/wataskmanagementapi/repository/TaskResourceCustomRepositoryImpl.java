package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskSearchSortProvider;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.SqlResultSetMapping;

@Slf4j
public class TaskResourceCustomRepositoryImpl implements TaskResourceCustomRepository {
    private static final String BASE_QUERY = "%sFROM {h-schema}tasks t "
                                             + "WHERE indexed "
                                             + "AND {h-schema}filter_signatures(t.task_id) && :filterSignature "
                                             + "AND {h-schema}role_signatures(t.task_id) && :roleSignature "
                                             + "%s%s%s";

    private static final String SELECT_CLAUSE = "SELECT t.task_id ";
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
        @Id int id;
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

        return query.getResultList();
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

        return ((Number) query.getSingleResult()).longValue();
    }

    void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    private String extraConstraints(List<String> excludeCaseIds, SearchRequest searchRequest) {
        StringBuilder extraConstraints = new StringBuilder("");
        if (searchRequest.isAvailableTasksOnly()) {
            extraConstraints.append("AND assignee IS NULL ");
        } else {
            extraConstraints.append(buildListConstraint(searchRequest.getUsers(), "assignee", "assignee", true));
        }
        if (CollectionUtils.isEmpty(searchRequest.getCftTaskStates())) {
            extraConstraints.append("AND cast(state as text) IN ('ASSIGNED','UNASSIGNED') ");
        } else {
            extraConstraints.append(buildListConstraint(searchRequest.getCftTaskStates(), "cast(state as text)",
                "state", true));
            //extraConstraints.append("AND state = cast(:state as {h-schema}task_state_enum) " );
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
            setParameter(query, "assignee", users);
        }
        if (!CollectionUtils.isEmpty(searchRequest.getCftTaskStates())) {
            List<String> states = searchRequest.getCftTaskStates()
                .stream()
                .map(CFTTaskState::getValue)
                .collect(Collectors.toList());
            setParameter(query, "state", states);
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