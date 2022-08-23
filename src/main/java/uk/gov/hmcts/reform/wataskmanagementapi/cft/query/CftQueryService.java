package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final TaskResourceDao taskResourceDao;

    private final AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    public CftQueryService(CamundaService camundaService,
                           CFTTaskMapper cftTaskMapper,
                           TaskResourceDao taskResourceDao,
                           AllowedJurisdictionConfiguration allowedJurisdictionConfiguration) {
        this.camundaService = camundaService;
        this.cftTaskMapper = cftTaskMapper;
        this.taskResourceDao = taskResourceDao;
        this.allowedJurisdictionConfiguration = allowedJurisdictionConfiguration;
    }

    public GetTasksResponse<Task> searchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        List<RoleAssignment> roleAssignments,
        List<PermissionTypes> permissionsRequired
    ) {
        validateRequest(searchTaskRequest);

        final List<Object[]> taskResourcesSummary = taskResourceDao.getTaskResourceSummary(
            firstResult,
            maxResults,
            searchTaskRequest,
            roleAssignments,
            permissionsRequired
        );

        if (isEmpty(taskResourcesSummary)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        final List<TaskResource> taskResources
            = taskResourceDao.getTaskResources(searchTaskRequest, taskResourcesSummary);

        Long count = taskResourceDao.getTotalCount(searchTaskRequest, roleAssignments, permissionsRequired);

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

        final List<TaskResource> taskResources = taskResourceDao.getCompletableTaskResources(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            taskTypes
        );

        final List<Task> tasks = mapTasksWithPermissionsUnion(roleAssignments, taskResources);
        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

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
        PermissionRequirements permissionRequirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(permissionsRequired.toArray(new PermissionTypes[0]));

        return getTask(taskId, roleAssignments, permissionRequirements);
    }

    public Optional<TaskResource> getTask(String taskId,
                                          List<RoleAssignment> roleAssignments,
                                          PermissionRequirements permissionRequirements
    ) {

        if (permissionRequirements.isEmpty()
            || taskId == null
            || taskId.isBlank()) {
            return Optional.empty();
        }

        return taskResourceDao.getTask(taskId, roleAssignments, permissionRequirements);
    }

    private List<Task> mapTasksWithPermissionsUnion(List<RoleAssignment> roleAssignments,
                                                    List<TaskResource> taskResources) {
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
