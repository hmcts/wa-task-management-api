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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TaskResourceCustomRepositoryImpl implements TaskResourceCustomRepository {
    private static final String BASE_QUERY =
        "%sFROM {h-schema}tasks t "
        + "WHERE indexed "
        + "AND {h-schema}filter_signatures(t.task_id, t.state, t.jurisdiction, t.role_category, t.work_type, "
        + "t.region, t.location) && CAST(:filterSignature AS text[]) "
        + "AND {h-schema}role_signatures(t.task_id, t.jurisdiction, t.region, t.location, t.case_id, "
        + "t.security_classification) && CAST(:roleSignature AS text[]) "
        + "%s%s%s";

    private static final String EMPTY_ROLE_CRITERIA_CTE =
        "WITH request_role_criteria AS ("
            + "SELECT CAST(NULL AS text) AS jurisdiction, "
            + "CAST(NULL AS text) AS region, "
            + "CAST(NULL AS text) AS location, "
            + "CAST(NULL AS text) AS role_name, "
            + "CAST(NULL AS text) AS case_id, "
            + "CAST(NULL AS text) AS permission, "
            + "CAST(NULL AS text) AS classification, "
            + "CAST(NULL AS text) AS authorization_value "
            + "WHERE false"
            + ") ";

    private static final String ROLE_PERMISSION_CONSTRAINT =
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

    private static final String BASE_QUERY_NEW =
        "%s%sFROM {h-schema}tasks t "
        + "WHERE indexed "
        + ROLE_PERMISSION_CONSTRAINT
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
                                       Collection<TaskSearchRoleCriteria> roleCriteria,
                                       List<String> excludeCaseIds,
                                       SearchRequest searchRequest) {

        RoleSearchCriteria searchRoleCriteria = buildRoleSearchCriteria(roleCriteria);
        String queryString = String.format(BASE_QUERY_NEW,
            searchRoleCriteria.cte(),
            SELECT_CLAUSE,
            extraConstraints(excludeCaseIds, searchRequest),
            TaskSearchSortProvider.getSortOrderQuery(searchRequest),
            PAGINATION_CLAUSE
        );

        log.info("Task search query [{}]", queryString);
        Query query = entityManager.createNativeQuery(queryString, RESULT_MAPPER);
        addParameters(query, firstResult, maxResults, searchRoleCriteria, excludeCaseIds, searchRequest);

        List<String> taskIds = query.getResultList();
        log.info("Number of tasks returned {}", CollectionUtils.isEmpty(taskIds) ? 0 : taskIds.size());

        return taskIds;
    }

    @SuppressWarnings("unchecked")
    private List<String> searchTasksIds(String baseQuery,
                                        int firstResult,
                                        int maxResults,
                                        Set<String> filterSignature,
                                        Set<String> roleSignature,
                                        List<String> excludeCaseIds,
                                        SearchRequest searchRequest) {
        String queryString = String.format(baseQuery,
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
    public List<String> searchTasksIdsOld(int firstResult,
                                          int maxResults,
                                          Set<String> filterSignature,
                                          Set<String> roleSignature,
                                          List<String> excludeCaseIds,
                                          SearchRequest searchRequest) {

        return searchTasksIds(BASE_QUERY, firstResult, maxResults, filterSignature, roleSignature,
                              excludeCaseIds, searchRequest);
    }

    @Override
    public Long searchTasksCount(Collection<TaskSearchRoleCriteria> roleCriteria,
                                 List<String> excludeCaseIds,
                                 SearchRequest searchRequest) {

        RoleSearchCriteria searchRoleCriteria = buildRoleSearchCriteria(roleCriteria);
        String queryString = String.format(BASE_QUERY_NEW,
            searchRoleCriteria.cte(),
            COUNT_CLAUSE,
            extraConstraints(excludeCaseIds, searchRequest),
            "", "");

        log.info("Task count query [{}]", queryString);
        Query query = entityManager.createNativeQuery(queryString);
        addParameters(query, searchRoleCriteria, excludeCaseIds, searchRequest);

        Long taskCount = ((Number) query.getSingleResult()).longValue();
        log.info("Total number of tasks {}", taskCount);

        return taskCount;
    }

    private Long searchTasksCount(String baseQuery,
                                  Set<String> filterSignature,
                                  Set<String> roleSignature,
                                  List<String> excludeCaseIds,
                                  SearchRequest searchRequest) {

        String queryString = String.format(baseQuery,
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

    @Override
    public Long searchTasksCountOld(Set<String> filterSignature,
                                    Set<String> roleSignature,
                                    List<String> excludeCaseIds,
                                    SearchRequest searchRequest) {

        return searchTasksCount(BASE_QUERY, filterSignature, roleSignature, excludeCaseIds, searchRequest);
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
            .append(buildListConstraint(searchRequest.getTaskTypes(), "task_type", "taskType", true))
            .append(buildListConstraint(searchRequest.getJurisdictions(), "jurisdiction", "jurisdiction", true))
            .append(buildListConstraint(searchRequest.getLocations(), "location", "location", true))
            .append(buildListConstraint(searchRequest.getRegions(), "region", "region", true))
            .append(buildListConstraint(searchRequest.getWorkTypes(), "work_type", "workType", true))
            .append(buildListConstraint(searchRequest.getRoleCategories(), "role_category", "roleCategory", true));
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

    private RoleSearchCriteria buildRoleSearchCriteria(Collection<TaskSearchRoleCriteria> roleCriteria) {
        if (CollectionUtils.isEmpty(roleCriteria)) {
            return new RoleSearchCriteria(EMPTY_ROLE_CRITERIA_CTE, List.of());
        }

        List<RoleCriterion> criteria = new ArrayList<>();
        for (TaskSearchRoleCriteria criterion : roleCriteria) {
            criteria.add(RoleCriterion.from(criteria.size(), criterion));
        }

        return new RoleSearchCriteria(buildRoleCriteriaCte(criteria), criteria);
    }

    private String buildRoleCriteriaCte(List<RoleCriterion> criteria) {
        String values = criteria.stream()
            .map(RoleCriterion::toSqlValues)
            .collect(Collectors.joining(", "));

        return "WITH request_role_criteria("
               + "jurisdiction, region, location, role_name, case_id, permission, classification, authorization_value"
               + ") AS (VALUES "
               + values
               + ") ";
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
                               int firstResult,
                               int maxResults,
                               RoleSearchCriteria roleCriteria,
                               List<String> excludeCaseIds,
                               SearchRequest searchRequest) {

        addParameters(query, roleCriteria, excludeCaseIds, searchRequest);
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

    private void addParameters(Query query,
                               RoleSearchCriteria roleCriteria,
                               List<String> excludeCaseIds,
                               SearchRequest searchRequest) {

        roleCriteria.setParameters(query);
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
            setParameter(query, "jurisdiction", jurisdictions);
        }
        List<String> locations = searchRequest.getLocations();
        if (!CollectionUtils.isEmpty(locations)) {
            setParameter(query, "location", locations);
        }
        List<String> regions = searchRequest.getRegions();
        if (!CollectionUtils.isEmpty(regions)) {
            setParameter(query, "region", regions);
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

    private record RoleSearchCriteria(String cte, List<RoleCriterion> criteria) {

        private void setParameters(Query query) {
            criteria.forEach(criterion -> criterion.setParameters(query));
        }
    }

    private record RoleCriterion(int index,
                                 String jurisdiction,
                                 String region,
                                 String location,
                                 String roleName,
                                 String caseId,
                                 String permission,
                                 String classification,
                                 String authorizationValue) {

        private static RoleCriterion from(int index, TaskSearchRoleCriteria roleCriteria) {
            return new RoleCriterion(
                index,
                roleCriteria.jurisdiction(),
                roleCriteria.region(),
                roleCriteria.location(),
                roleCriteria.roleName(),
                roleCriteria.caseId(),
                roleCriteria.permission(),
                roleCriteria.classification(),
                roleCriteria.authorizationValue()
            );
        }

        private String toSqlValues() {
            return "("
                   + sqlValue("roleJurisdiction", jurisdiction) + ", "
                   + sqlValue("roleRegion", region) + ", "
                   + sqlValue("roleLocation", location) + ", "
                   + sqlValue("roleName", roleName) + ", "
                   + sqlValue("roleCaseId", caseId) + ", "
                   + sqlValue("rolePermission", permission) + ", "
                   + sqlValue("roleClassification", classification) + ", "
                   + sqlValue("roleAuthorization", authorizationValue)
                   + ")";
        }

        private void setParameters(Query query) {
            setParameter(query, "roleJurisdiction", jurisdiction);
            setParameter(query, "roleRegion", region);
            setParameter(query, "roleLocation", location);
            setParameter(query, "roleName", roleName);
            setParameter(query, "roleCaseId", caseId);
            setParameter(query, "rolePermission", permission);
            setParameter(query, "roleClassification", classification);
            setParameter(query, "roleAuthorization", authorizationValue);
        }

        private String sqlValue(String name, String value) {
            return value == null ? "CAST(NULL AS text)" : "CAST(:" + name + index + " AS text)";
        }

        private void setParameter(Query query, String name, String value) {
            if (value != null) {
                query.setParameter(name + index, value);
            }
        }
    }
}
