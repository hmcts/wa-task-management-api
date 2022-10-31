package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.extern.slf4j.Slf4j;
import net.hmcts.taskperf.service.TaskSearchAdaptor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.sql.SQLException;
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

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class CftQueryService {
    private final CamundaService camundaService;
    private final CFTTaskMapper cftTaskMapper;
    private final TaskResourceDao taskResourceDao;

    private final AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    private final TaskSearchAdaptor taskSearchAdaptor;

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
            AccessControlResponse accessControlResponse,
            boolean granularPermissionResponseFeature) {
        if (taskSearchAdaptor.isEnabled()) {
            try {
                return taskSearchAdaptor.searchForTasks(firstResult, maxResults, searchTaskRequest,
                                                        accessControlResponse.getRoleAssignments(),
                                                        granularPermissionResponseFeature);
            } catch (SQLException e) {
                log.error("POC Database connection error {}", e.getMessage());
                return new GetTasksResponse<>(List.of(), 0);
            }
        } else {
            return originalSearchForTasks(firstResult, maxResults, searchTaskRequest, accessControlResponse,
                                          granularPermissionResponseFeature);
        }
    }

    private GetTasksResponse<Task> originalSearchForTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        List<RoleAssignment> roleAssignments,
        List<PermissionTypes> permissionsRequired,
        boolean granularPermissionResponseFeature
    ) {
        boolean isGranularPermissionEnabled = launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.GRANULAR_PERMISSION_FEATURE,
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );

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
            if (context == null) {
                return PermissionRequirementBuilder.builder().buildSingleType(READ);
            } else if (context.equals(RequestContext.AVAILABLE_TASKS)) {
                return PermissionRequirementBuilder.builder().buildSingleRequirementWithAnd(OWN, CLAIM);
            } else if (context.equals(RequestContext.ALL_WORK)) {
                return PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
            }
            return PermissionRequirementBuilder.builder().buildSingleType(READ);
        } else {
            if (searchTaskRequest.isAvailableTasksOnly()) {
                return PermissionRequirementBuilder.builder().buildSingleRequirementWithAnd(OWN, READ);
            } else {
                return PermissionRequirementBuilder.builder().buildSingleType(READ);
            }
        }
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

}
