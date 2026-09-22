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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskSearchSortProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentFilter.ONE;

@Slf4j
public class TaskResourceCustomRepositoryImpl implements TaskResourceCustomRepository {

    private static final String SELECT_CLAUSE = "SELECT t.task_id ";

    private static final String DB_COL_ASSIGNEE = "assignee";

    private static final String DB_COL_JURISDICTION = "jurisdiction";

    private static final String DB_COL_REGION = "region";

    private static final String DB_COL_LOCATION = "location";
    private static final String COUNT_CLAUSE = "SELECT count(*) ";
    private static final String PAGINATION_CLAUSE = "OFFSET :firstResult LIMIT :maxResults";
    protected static final String RESULT_MAPPER = "TaskSearchResult";
    private static final String TASK_ROLE_QUERY = """
        %s
        FROM {h-schema}tasks t
        WHERE t.indexed
          AND t.state IN ('ASSIGNED', 'UNASSIGNED')
          AND t.security_classification = ANY(
              CAST(:taskRoleClassifications AS {h-schema}security_classification_enum[])
          )
          %s
          AND (%s)
        """;

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

    void setEntityManager(EntityManager em) {
        this.entityManager = em;
    }

    private static final String BASE_QUERY =
        "%sFROM {h-schema}tasks t "
        + "WHERE indexed "
        + "AND {h-schema}filter_signatures(t.task_id, t.state, t.jurisdiction, t.role_category, t.work_type, "
        + "t.region, t.location) && CAST(:filterSignature AS text[]) "
        + "AND {h-schema}role_signatures(t.task_id, t.jurisdiction, t.region, t.location, t.case_id, "
        + "t.security_classification) && CAST(:roleSignature AS text[]) "
        + "%s%s%s";

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchTasksIdsUsingTaskRoles(int firstResult,
                                                     int maxResults,
                                                     Collection<TaskSearchRoleCriteria> roleCriteria,
                                                     List<String> excludeCaseIds,
                                                     SearchRequest searchRequest) {
        TaskRoleSearchPredicate rolePredicate = TaskRoleSearchPredicate.forPage(roleCriteria);
        String queryString = taskRoleQuery(SELECT_CLAUSE, rolePredicate, excludeCaseIds, searchRequest)
            + TaskSearchSortProvider.getSortOrderQuery(searchRequest) + PAGINATION_CLAUSE;

        Query query = entityManager.createNativeQuery(queryString, RESULT_MAPPER);
        rolePredicate.setParameters(query);
        addSearchRequestParameters(query, excludeCaseIds, searchRequest);
        query.setParameter("firstResult", firstResult);
        query.setParameter("maxResults", maxResults);
        return query.getResultList();
    }

    @Override
    public Long searchTasksCountUsingTaskRoles(Collection<TaskSearchRoleCriteria> roleCriteria,
                                               List<String> excludeCaseIds,
                                               SearchRequest searchRequest) {
        TaskRoleSearchPredicate rolePredicate = TaskRoleSearchPredicate.from(roleCriteria);
        String queryString = taskRoleQuery(COUNT_CLAUSE, rolePredicate, excludeCaseIds, searchRequest);

        Query query = entityManager.createNativeQuery(queryString);
        rolePredicate.setParameters(query);
        addSearchRequestParameters(query, excludeCaseIds, searchRequest);
        return ((Number) query.getSingleResult()).longValue();
    }

    private String taskRoleQuery(String selectClause,
                                 TaskRoleSearchPredicate rolePredicate,
                                 List<String> excludeCaseIds,
                                 SearchRequest searchRequest) {
        return TASK_ROLE_QUERY.formatted(
            selectClause, extraConstraints(excludeCaseIds, searchRequest, "t."), rolePredicate.sql());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchTasksIdsUsingSearchIndex(int firstResult,
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

        Query query = entityManager.createNativeQuery(queryString, RESULT_MAPPER);
        addParameters(query, firstResult, maxResults, filterSignature, roleSignature, excludeCaseIds, searchRequest);

        List<String> taskIds = query.getResultList();
        log.info("Number of tasks returned {}", CollectionUtils.isEmpty(taskIds) ? 0 : taskIds.size());

        return taskIds;
    }

    @Override
    public Long searchTasksCountUsingSearchIndex(Set<String> filterSignature,
                                                 Set<String> roleSignature,
                                                 List<String> excludeCaseIds,
                                                 SearchRequest searchRequest) {

        String queryString = String.format(BASE_QUERY,
                                           COUNT_CLAUSE,
                                           extraConstraints(excludeCaseIds, searchRequest),
                                           "", "");

        Query query = entityManager.createNativeQuery(queryString);
        addParameters(query, filterSignature, roleSignature, excludeCaseIds, searchRequest);

        Long taskCount = ((Number) query.getSingleResult()).longValue();
        log.info("Total number of tasks {}", taskCount);

        return taskCount;
    }

    private String extraConstraints(List<String> excludeCaseIds, SearchRequest searchRequest) {
        return extraConstraints(excludeCaseIds, searchRequest, "");
    }

    private String extraConstraints(List<String> excludeCaseIds, SearchRequest searchRequest, String tableAlias) {
        StringBuilder extraConstraints = new StringBuilder("");
        if (searchRequest.isAvailableTasksOnly()) {
            extraConstraints.append("AND ").append(tableAlias).append(DB_COL_ASSIGNEE).append(" IS NULL ");
        } else {
            extraConstraints.append(buildListConstraint(searchRequest.getUsers(),
                                                        tableAlias + DB_COL_ASSIGNEE, DB_COL_ASSIGNEE, true));
        }
        if (CollectionUtils.isEmpty(searchRequest.getCftTaskStates())) {
            extraConstraints.append("AND ").append(tableAlias).append("state IN ('ASSIGNED', 'UNASSIGNED') ");
        } else {
            String states = searchRequest.getCftTaskStates()
                .stream()
                .map(s -> "'" + s.getValue() + "'")
                .collect(Collectors.joining(", "));
            extraConstraints.append("AND ").append(tableAlias).append("state IN (").append(states).append(") ");
        }
        extraConstraints.append(buildListConstraint(searchRequest.getCaseIds(), tableAlias + "case_id", "caseId", true))
            .append(buildListConstraint(excludeCaseIds, tableAlias + "case_id", "excludedCaseId", false))
            .append(buildListConstraint(searchRequest.getTaskTypes(), tableAlias + "task_type", "taskType", true))
            .append(buildListConstraint(searchRequest.getJurisdictions(),
                                        tableAlias + DB_COL_JURISDICTION, DB_COL_JURISDICTION, true))
            .append(buildListConstraint(searchRequest.getLocations(),
                                        tableAlias + DB_COL_LOCATION, DB_COL_LOCATION, true))
            .append(buildListConstraint(searchRequest.getRegions(), tableAlias + DB_COL_REGION, DB_COL_REGION, true))
            .append(buildListConstraint(searchRequest.getWorkTypes(), tableAlias + "work_type", "workType", true))
            .append(buildListConstraint(searchRequest.getRoleCategories(),
                                        tableAlias + "role_category", "roleCategory", true));
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
        addSearchRequestParameters(query, excludeCaseIds, searchRequest);
    }

    private void addSearchRequestParameters(Query query,
                                            List<String> excludeCaseIds,
                                            SearchRequest searchRequest) {

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
        List<String> jurisdictions = searchRequest.getJurisdictions();
        if (!CollectionUtils.isEmpty(jurisdictions)) {
            setParameter(query, DB_COL_JURISDICTION, jurisdictions);
        }
        List<String> locations = searchRequest.getLocations();
        if (!CollectionUtils.isEmpty(locations)) {
            setParameter(query, DB_COL_LOCATION, locations);
        }
        List<String> regions = searchRequest.getRegions();
        if (!CollectionUtils.isEmpty(regions)) {
            setParameter(query, DB_COL_REGION, regions);
        }
        List<String> workTypes = searchRequest.getWorkTypes();
        if (!CollectionUtils.isEmpty(workTypes)) {
            setParameter(query, "workType", workTypes);
        }
        List<RoleCategory> roleCategories = searchRequest.getRoleCategories();
        if (!CollectionUtils.isEmpty(roleCategories)) {
            setParameter(query, "roleCategory", roleCategories.stream().map(RoleCategory::name).toList());
        }
        if (!CollectionUtils.isEmpty(excludeCaseIds)) {
            setParameter(query, "excludedCaseId", excludeCaseIds);
        }
    }

    private void setParameter(Query query, String name, List<String> values) {
        query.setParameter(name, values.size() == ONE ? values.get(0) : values);
    }

}
