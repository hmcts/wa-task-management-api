package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class CftQueryService {
    public static final List<String> ALLOWED_WORK_TYPES = List.of(
        "hearing_work", "upper_tribunal", "routine_work", "decision_making_work",
        "applications", "priority", "access_requests", "error_management",
        "review_case", "evidence", "follow_up"
    );

    private final CamundaService camundaService;
    private final CFTTaskMapper cftTaskMapper;
    private final TaskResourceDao taskResourceDao;

    private final AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    public CftQueryService(CamundaService camundaService,
                           CFTTaskMapper cftTaskMapper,
                           TaskResourceDao taskResourceDao,
                           AllowedJurisdictionConfiguration allowedJurisdictionConfiguration,
                           LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider) {
        this.camundaService = camundaService;
        this.cftTaskMapper = cftTaskMapper;
        this.taskResourceDao = taskResourceDao;
        this.allowedJurisdictionConfiguration = allowedJurisdictionConfiguration;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
    }

    public GetTasksResponse<Task> searchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        boolean granularPermissionResponseFeature
    ) {
        boolean isGranularPermissionEnabled = launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.GRANULAR_PERMISSION_FEATURE,
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );
        log.info("Granular permission feature flag value '{}'", isGranularPermissionEnabled);
        validateRequest(searchTaskRequest, isGranularPermissionEnabled);

        List<RoleAssignment> roleAssignments = accessControlResponse.getRoleAssignments();
        PermissionRequirements permissionsRequired = findPermissionRequirement(searchTaskRequest,
                                                                               isGranularPermissionEnabled);
        boolean availableTasksOnly = isAvailableTasksOnly(searchTaskRequest);

        final List<Object[]> taskResourcesSummary = taskResourceDao.getTaskResourceSummary(
            firstResult,
            maxResults,
            searchTaskRequest,
            roleAssignments,
            permissionsRequired,
            availableTasksOnly
        );

        if (isEmpty(taskResourcesSummary)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        final List<TaskResource> taskResources
            = taskResourceDao.getTaskResources(searchTaskRequest, taskResourcesSummary);

        Long count = taskResourceDao.getTotalCount(searchTaskRequest,
                                                   roleAssignments,
                                                   permissionsRequired,
                                                   availableTasksOnly);

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                     cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                         taskResource,
                         roleAssignments,
                         granularPermissionResponseFeature
                     )
            )
            .collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, count);
    }

    public GetTasksCompletableResponse<Task> searchForCompletableTasks(
        SearchEventAndCase searchEventAndCase,
        List<RoleAssignment> roleAssignments,
        PermissionRequirements permissionsRequired,
        boolean granularPermissionResponseFeature
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

        final List<Task> tasks = mapTasksWithPermissionsUnion(roleAssignments, taskResources,
                                                              granularPermissionResponseFeature);
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

    private PermissionRequirements findPermissionRequirement(SearchTaskRequest searchTaskRequest,
                                                             boolean isGranularPermissionEnabled) {
        if (isGranularPermissionEnabled) {
            //When granular permission feature flag is enabled, request is expected only in new format
            RequestContext context = searchTaskRequest.getRequestContext();
            log.info("Request context value received: '{}'", context);
            if (context == null) {
                return PermissionRequirementBuilder.builder().buildSingleType(READ);
            } else if (context.equals(RequestContext.AVAILABLE_TASKS)) {
                return PermissionRequirementBuilder.builder().buildSingleRequirementWithAnd(OWN, CLAIM);
            } else if (context.equals(RequestContext.ALL_WORK)) {
                return PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
            }
            return PermissionRequirementBuilder.builder().buildSingleType(READ);
        } else {
            if (isAvailableTasksOnly(searchTaskRequest)) {
                return PermissionRequirementBuilder.builder().buildSingleRequirementWithAnd(OWN, READ);
            } else {
                return PermissionRequirementBuilder.builder().buildSingleType(READ);
            }
        }
    }

    // TODO: Once the granular permission feature flag enabled or available_tasks_only parameter is depreciated,
    // this method should only check AVAILABLE_TASK_ONLY context
    private boolean isAvailableTasksOnly(SearchTaskRequest searchTaskRequest) {
        final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap = asEnumMapForBoolean(searchTaskRequest);
        SearchParameterBoolean availableTasksOnly = boolKeyMap.get(AVAILABLE_TASKS_ONLY);

        RequestContext context = searchTaskRequest.getRequestContext();

        if (context == null) {
            return availableTasksOnly != null && availableTasksOnly.getValues();
        } else {
            return context.equals(RequestContext.AVAILABLE_TASKS);
        }
    }

    private static EnumMap<SearchParameterKey, SearchParameterBoolean> asEnumMapForBoolean(
        SearchTaskRequest searchTaskRequest) {

        EnumMap<SearchParameterKey, SearchParameterBoolean> map = new EnumMap<>(SearchParameterKey.class);
        if (searchTaskRequest != null && !CollectionUtils.isEmpty(searchTaskRequest.getSearchParameters())) {
            searchTaskRequest.getSearchParameters()
                .stream()
                .filter(SearchParameterBoolean.class::isInstance)
                .forEach(request -> map.put(request.getKey(), (SearchParameterBoolean) request));
        }

        return map;
    }

    private List<Task> mapTasksWithPermissionsUnion(List<RoleAssignment> roleAssignments,
                                                    List<TaskResource> taskResources,
                                                    boolean granularPermissionResponseFeature) {
        if (taskResources.isEmpty()) {
            return emptyList();
        }

        return taskResources.stream()
            .map(taskResource -> cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                     taskResource,
                     roleAssignments,
                     granularPermissionResponseFeature
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

    private void validateRequest(SearchTaskRequest searchTaskRequest, boolean isGranularPermissionEnabled) {
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

        if (isGranularPermissionEnabled) {
            final EnumMap<SearchParameterKey, SearchParameterBoolean> boolKeyMap =
                asEnumMapForBoolean(searchTaskRequest);
            if (boolKeyMap.containsKey(AVAILABLE_TASKS_ONLY)) {
                violations.add(new Violation(AVAILABLE_TASKS_ONLY.value(), "Invalid request parameter"));
            }
        }

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
    }
}
