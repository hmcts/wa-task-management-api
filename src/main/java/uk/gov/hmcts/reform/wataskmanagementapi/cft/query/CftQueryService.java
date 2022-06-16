package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import net.hmcts.taskperf.service.TaskSearchAdaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResourceSummary;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
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

import java.sql.SQLException;
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
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class CftQueryService {
    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management"
    );

    private final CamundaService camundaService;
    private final CFTTaskMapper cftTaskMapper;

    @PersistenceContext
    private final EntityManager entityManager;

    private final TaskSearchAdaptor taskSearchAdaptor;

    private final AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    public CftQueryService(CamundaService camundaService,
                           CFTTaskMapper cftTaskMapper,
                           EntityManager entityManager,
                           AllowedJurisdictionConfiguration allowedJurisdictionConfiguration,
                           TaskSearchAdaptor taskSearchAdaptor) {
        this.camundaService = camundaService;
        this.cftTaskMapper = cftTaskMapper;
        this.entityManager = entityManager;
        this.taskSearchAdaptor = taskSearchAdaptor;
        this.allowedJurisdictionConfiguration = allowedJurisdictionConfiguration;
    }

    public GetTasksResponse<Task> searchForTasks(
            int firstResult,
            int maxResults,
            SearchTaskRequest searchTaskRequest,
            List<RoleAssignment> roleAssignments,
            List<PermissionTypes> permissionsRequired) {
        if (taskSearchAdaptor.isEnabled()) {
            try {
                return taskSearchAdaptor.searchForTasks(firstResult, maxResults, searchTaskRequest, roleAssignments,
                                                        permissionsRequired);
            } catch (SQLException e) {
                log.error("POC Database connection error {}", e.getMessage());
                return new GetTasksResponse<>(List.of(), 0);
            }
        } else {
            return originalSearchForTasks(firstResult, maxResults, searchTaskRequest, roleAssignments,
                                          permissionsRequired);
        }
    }

    private GetTasksResponse<Task> originalSearchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        List<RoleAssignment> roleAssignments,
        List<PermissionTypes> permissionsRequired
    ) {
        validateRequest(searchTaskRequest);

        final List<TaskResourceSummary> taskResourcesSummary = getTaskResourceSummary(
            firstResult,
            maxResults,
            searchTaskRequest,
            roleAssignments,
            permissionsRequired
        );

        if (isEmpty(taskResourcesSummary)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        final List<TaskResource> taskResources = getTaskResources(searchTaskRequest, taskResourcesSummary);

        Long count = getTotalCount(searchTaskRequest, roleAssignments, permissionsRequired);

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                     cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                         taskResource,
                         roleAssignments
                     )
            )
            .collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, count);
    }

    private List<TaskResourceSummary> getTaskResourceSummary(int firstResult,
                                                             int maxResults,
                                                             SearchTaskRequest searchTaskRequest,
                                                             List<RoleAssignment> roleAssignments,
                                                             List<PermissionTypes> permissionsRequired) {
        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        Pageable page = OffsetPageableRequest.of(firstResult, maxResults, sort);
        TaskResourceSummaryQueryBuilder summaryQueryBuilder = new TaskResourceSummaryQueryBuilder(entityManager);
        CriteriaBuilder builder = summaryQueryBuilder.builder;
        Root<TaskResource> root = summaryQueryBuilder.root;

        List<Order> orders = SortQuery.sortByFields(searchTaskRequest, builder, root);
        Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            builder,
            root
        );

        return summaryQueryBuilder
            .where(selectPredicate)
            .withOrders(orders)
            .build()
            .setFirstResult((int) page.getOffset())
            .setMaxResults(page.getPageSize())
            .getResultList();

    }

    private List<TaskResource> getTaskResources(SearchTaskRequest searchTaskRequest,
                                                List<TaskResourceSummary> taskResourcesSummary) {
        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager, true);
        CriteriaBuilder builder = selectQueryBuilder.builder;
        Root<TaskResource> root = selectQueryBuilder.root;

        List<String> taskIds = taskResourcesSummary.stream()
            .map(TaskResourceSummary::getTaskId)
            .collect(Collectors.toList());
        List<Order> orders = SortQuery.sortByFields(searchTaskRequest, builder, root);
        Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskQuery(taskIds, builder, root);

        return selectQueryBuilder
            .where(selectPredicate)
            .withOrders(orders)
            .build()
            .getResultList();
    }

    public GetTasksCompletableResponse<Task> searchForCompletableTasks(
        SearchEventAndCase searchEventAndCase,
        List<RoleAssignment> roleAssignments,
        List<PermissionTypes> permissionsRequired
    ) {

        //Safe-guard against unsupported Jurisdictions.
        if (!allowedJurisdictionConfiguration.getAllowedJurisdictions()
            .contains(searchEventAndCase.getCaseJurisdiction().toLowerCase(Locale.ROOT))
            || !allowedJurisdictionConfiguration.getAllowedCaseTypes()
            .contains(searchEventAndCase.getCaseType().toLowerCase(Locale.ROOT))
        ) {
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

        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);

        final Predicate selectPredicate = TaskSearchQueryBuilder.buildQueryForCompletable(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            taskTypes,
            selectQueryBuilder.builder,
            selectQueryBuilder.root
        );

        TypedQuery<TaskResource> build = selectQueryBuilder
            .where(selectPredicate)
            .build();
        final List<TaskResource> taskResources = build
            .getResultList();

        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

        final List<Task> tasks = getTasks(roleAssignments, taskResources);

        return new GetTasksCompletableResponse<>(taskRequiredForEvent, tasks);
    }

    public Optional<TaskResource> getTask(String taskId,
                                          List<RoleAssignment> roleAssignments,
                                          List<PermissionTypes> permissionsRequired
    ) {

        if (permissionsRequired.isEmpty()
            || taskId == null
            || taskId.isBlank()) {
            return Optional.empty();
        }

        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);

        final Predicate selectPredicate = TaskSearchQueryBuilder.buildSingleTaskQuery(
            taskId,
            roleAssignments,
            permissionsRequired,
            selectQueryBuilder.builder,
            selectQueryBuilder.root
        );

        selectQueryBuilder.where(selectPredicate).build().getResultList();

        try {
            return selectQueryBuilder
                .where(selectPredicate)
                .build()
                .getResultList().stream()
                .findFirst();
        } catch (NoResultException ne) {
            return Optional.empty();
        }
    }

    private Long getTotalCount(SearchTaskRequest searchTaskRequest,
                               List<RoleAssignment> roleAssignments,
                               List<PermissionTypes> permissionsRequired) {

        CountTaskResourceQueryBuilder countQueryBuilder = new CountTaskResourceQueryBuilder(entityManager)
            .createSubQuery()
            .createSubRoot();

        Predicate countPredicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            countQueryBuilder.builder,
            countQueryBuilder.subRoot
        );

        return countQueryBuilder
            .where(countPredicate)
            .build()
            .getSingleResult();
    }

    private List<Task> getTasks(List<RoleAssignment> roleAssignments, List<TaskResource> taskResources) {
        if (taskResources.isEmpty()) {
            return emptyList();
        }

        return taskResources.stream()
            .map(taskResource -> cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                     taskResource,
                     roleAssignments
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
