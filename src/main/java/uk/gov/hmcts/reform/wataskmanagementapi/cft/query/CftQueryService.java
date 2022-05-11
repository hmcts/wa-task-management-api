package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryFullyQualifiedName"})
public class CftQueryService {
    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management"
    );

    private final CamundaService camundaService;
    private final CFTTaskMapper cftTaskMapper;

    @PersistenceContext
    private final EntityManager entityManager;

    public CftQueryService(CamundaService camundaService,
                           CFTTaskMapper cftTaskMapper,
                           EntityManager entityManager) {
        this.camundaService = camundaService;
        this.cftTaskMapper = cftTaskMapper;
        this.entityManager = entityManager;
    }

    public GetTasksResponse<Task> searchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {
        validateRequest(searchTaskRequest);

        //TODO this should pass only prepared data.
        // Role Assignments that are filtered and grouped already.
        // Should also be in one object called SearchData
        //SearchData searchData = new TaskSearchData(
        //    new AndPermissionsRequired(permissionsRequired),
        //    Lists.newArrayList(),//TODO put RoleAssignmentForSearch list in there
        //    searchTaskRequest);
        //
        //final Specification<TaskResource> taskQuerySpecification = TaskSearchQueryBuilder.build(searchData);
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TaskResource> criteriaQuery = criteriaBuilder.createQuery(TaskResource.class);
        Root<TaskResource> root = criteriaQuery.from(TaskResource.class);

        criteriaQuery.where(TaskSearchQueryBuilder.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired,
            criteriaBuilder,
            root
        ));

        criteriaQuery.distinct(true);

        criteriaQuery.orderBy(SortQuery.sortByFields(searchTaskRequest, criteriaBuilder, root));

        TypedQuery<TaskResource> query = entityManager.createQuery(criteriaQuery);

        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        Pageable page = OffsetPageableRequest.of(firstResult, maxResults, sort);

        query.setFirstResult((int) page.getOffset())
            .setMaxResults(page.getPageSize());

        final List<TaskResource> taskResources = query.getResultList();

        Long count = getCount(searchTaskRequest, accessControlResponse, permissionsRequired, firstResult);

        //final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        //final List<TaskResource> taskResources = pages.toList();

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                     cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                         taskResource,
                         accessControlResponse.getRoleAssignments()
                     )
            )
            .collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, count);
    }

    private Long getCount(SearchTaskRequest searchTaskRequest,
                          AccessControlResponse accessControlResponse,
                          List<PermissionTypes> permissionsRequired,
                          int firstResult) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<TaskResource> taskResourcesRootCount = countQuery.from(TaskResource.class);
        countQuery.select(criteriaBuilder.countDistinct(taskResourcesRootCount));
        countQuery.where(TaskSearchQueryBuilder.buildTaskQuery(
            searchTaskRequest,
            accessControlResponse,
            permissionsRequired,
            criteriaBuilder,
            taskResourcesRootCount
        ));
        countQuery.distinct(true);
        TypedQuery<Long> typedQuery = entityManager.createQuery(countQuery);
        typedQuery.setFirstResult(firstResult);
        return typedQuery.getSingleResult();
    }

    public GetTasksCompletableResponse<Task> searchForCompletableTasks(
        SearchEventAndCase searchEventAndCase,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {

        //Safe-guard against unsupported Jurisdictions and case types.
        if (!Arrays.asList("IA", "WA")
            .contains(searchEventAndCase.getCaseJurisdiction().toUpperCase(Locale.ROOT))
            || !Arrays.asList("asylum", "wacasetype")
            .contains(searchEventAndCase.getCaseType().toLowerCase(Locale.ROOT))) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        //1. Evaluate Dmn
        final List<Map<String, CamundaVariable>> evaluateDmnResult = camundaService.evaluateTaskCompletionDmn(
            searchEventAndCase);

        // Collect task types
        List<String> taskTypes = extractTaskTypes(evaluateDmnResult);

        if (taskTypes.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<TaskResource> criteriaQuery = criteriaBuilder.createQuery(TaskResource.class);
        Root<TaskResource> root = criteriaQuery.from(TaskResource.class);

        final Predicate taskResourceSpecification = TaskSearchQueryBuilder
            .buildQueryForCompletable(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired,
                taskTypes,
                criteriaBuilder,
                root
            );

        //final List<TaskResource> taskResources = taskResourceRepository.findAll(taskResourceSpecification);
        criteriaQuery.distinct(true);
        criteriaQuery.where(taskResourceSpecification);

        final List<TaskResource> taskResources = entityManager.createQuery(criteriaQuery).getResultList();

        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

        final List<Task> tasks = getTasks(accessControlResponse, taskResources);

        return new GetTasksCompletableResponse<>(taskRequiredForEvent, tasks);
    }

    public Optional<TaskResource> getTask(String taskId,
                                          AccessControlResponse accessControlResponse,
                                          List<PermissionTypes> permissionsRequired
    ) {

        if (permissionsRequired.isEmpty()
            || taskId == null
            || taskId.isBlank()) {
            return Optional.empty();
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<TaskResource> criteriaQuery = builder.createQuery(TaskResource.class);
        Root<TaskResource> root = criteriaQuery.from(TaskResource.class);

        final Predicate taskResourceSpecification = TaskSearchQueryBuilder
            .buildSingleTaskQuery(taskId, accessControlResponse, permissionsRequired, builder, root);

        criteriaQuery.where(taskResourceSpecification);

        try {
            return Optional.of(entityManager.createQuery(criteriaQuery).getSingleResult());
        } catch (NoResultException ne) {
            return Optional.empty();
        }
    }

    private List<Task> getTasks(AccessControlResponse accessControlResponse, List<TaskResource> taskResources) {
        if (taskResources.isEmpty()) {
            return emptyList();
        }

        return taskResources.stream()
            .map(taskResource -> cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                     taskResource,
                     accessControlResponse.getRoleAssignments()
                 )
            )
            .collect(Collectors.toList());
    }

    private List<String> extractTaskTypes(List<Map<String, CamundaVariable>> evaluateDmnResult) {
        return evaluateDmnResult.stream()
            .filter(result -> result.containsKey(TASK_TYPE.value()))
            .map(result -> camundaService.getVariableValue(result.get(TASK_TYPE.value()), String.class))
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());

    }

    private boolean isTaskRequired(List<Map<String, CamundaVariable>> evaluateDmnResult, List<String> taskTypes) {
        /*
         * EvaluateDmnResult contains with and without empty rows for an event.
         * TaskTypes are extracted from evaluateDmnResult.
         * If both the sizes are equal, it means there is no empty row and task is required for the event
         * If they are of different sizes, it means there is an empty row and task is not required
         */
        return evaluateDmnResult.size() == taskTypes.size();
    }

    private void validateRequest(SearchTaskRequest searchTaskRequest) {
        List<Violation> violations = new ArrayList<>();

        //Validate work-type
        List<SearchParameterList> workType = new ArrayList<>();
        for (SearchParameter<?> sp : searchTaskRequest.getSearchParameters()) {
            if (sp.getKey().equals(SearchParameterKey.WORK_TYPE)) {
                workType.add((SearchParameterList) sp);
            }
        }

        if (!workType.isEmpty()) {
            //validate work type
            SearchParameterList workTypeParameter = workType.get(0);
            List<String> values = workTypeParameter.getValues();
            //Validate
            values.forEach(value -> {
                if (!ALLOWED_WORK_TYPES.contains(value)) {
                    violations.add(new Violation(
                        value,
                        workTypeParameter.getKey() + " must be one of " + Arrays.toString(ALLOWED_WORK_TYPES.toArray())
                    ));
                }
            });
        }

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
    }
}
