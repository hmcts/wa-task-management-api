package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.nimbusds.oauth2.sdk.util.CollectionUtils.isEmpty;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;

@Slf4j
@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class CftQueryService {

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
        SearchRequest searchRequest,
        AccessControlResponse accessControlResponse
    ) {

        List<RoleAssignment> roleAssignments = accessControlResponse.getRoleAssignments();
        PermissionRequirements permissionsRequired = findPermissionRequirement(searchRequest);
        boolean availableTasksOnly = searchRequest.isAvailableTasksOnly();

        final List<Object[]> taskResourcesSummary = taskResourceDao.getTaskResourceSummary(
            firstResult,
            maxResults,
            searchRequest,
            roleAssignments,
            permissionsRequired,
            availableTasksOnly
        );

        if (isEmpty(taskResourcesSummary)) {
            return new GetTasksResponse<>(List.of(), 0);
        }

        final List<TaskResource> taskResources
            = taskResourceDao.getTaskResources(searchRequest, taskResourcesSummary);

        //There is an issue with the query involving count with Hibernate.
        //Hibernate search is not being used in prod. There is a plan to remove the unused code, in the future.
        //PDT's are using test controller for verifying the tasks.
        //The test controller is still invoking the hibernate search queries.
        //To progress with the springboot upgrade,
        // the hibernate query was bypassed and acquiring the count from the taskResources.
        long count = taskResources.stream().count();

        final List<Task> tasks = taskResources.stream()
            .map(taskResource ->
                cftTaskMapper.mapToTaskAndExtractPermissionsUnion(
                    taskResource,
                    roleAssignments
                )
            )
            .toList();

        return new GetTasksResponse<>(tasks, count);
    }

    public GetTasksCompletableResponse<Task> searchForCompletableTasks(
        SearchEventAndCase searchEventAndCase,
        List<RoleAssignment> roleAssignments,
        PermissionRequirements permissionsRequired
    ) {
        if (!isAllowedForJurisdictionAndCaseType(searchEventAndCase)) {
            log.info("Jurisdiction: \"{}\" or CaseType: \"{}\" not supported",
                     searchEventAndCase.getCaseJurisdiction(), searchEventAndCase.getCaseType());
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        List<TaskResource> apiFirstTaskResources =
            getApiFirstCompletableTaskResources(searchEventAndCase, roleAssignments, permissionsRequired);

        CamundaCompletableTasks camundaCompletableTasks =
            getCamundaCompletableTasks(searchEventAndCase, roleAssignments, permissionsRequired);

        List<TaskResource> taskResources = mergeTaskResources(
            camundaCompletableTasks.taskResources(),
            apiFirstTaskResources
        );
        final List<Task> tasks = mapTasksWithPermissionsUnion(roleAssignments, taskResources);

        boolean taskRequiredForEvent = camundaCompletableTasks.taskRequiredForEvent() ||
            isApiFirstTaskRequiredForEvent(apiFirstTaskResources, searchEventAndCase);

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

    private PermissionRequirements findPermissionRequirement(SearchRequest searchRequest) {

        if (searchRequest.isAvailableTasksOnly()) {
            return PermissionRequirementBuilder.builder().buildSingleRequirementWithAnd(OWN, CLAIM);
        } else if (searchRequest.isAllWork()) {
            return PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        } else {
            return PermissionRequirementBuilder.builder().buildSingleType(READ);
        }
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
            .toList();
    }

    private List<String> extractTaskTypes(List<Map<String, CamundaVariable>> evaluateDmnResult) {
        return evaluateDmnResult.stream()
            .filter(result -> result.containsKey(TASK_TYPE.value()))
            .map(result -> camundaService.getVariableValue(result.get(TASK_TYPE.value()), String.class))
            .filter(StringUtils::hasText)
            .toList();

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

    private boolean isAllowedForJurisdictionAndCaseType(SearchEventAndCase searchEventAndCase) {
        List<String> allowedJurisdictions = Optional.ofNullable(allowedJurisdictionConfiguration.getAllowedJurisdictions())
            .orElse(emptyList());
        List<String> allowedCaseTypes = Optional.ofNullable(allowedJurisdictionConfiguration.getAllowedCaseTypes())
            .orElse(emptyList());

        return allowedJurisdictions.contains(searchEventAndCase.getCaseJurisdiction().toLowerCase(Locale.ROOT))
            && allowedCaseTypes.contains(searchEventAndCase.getCaseType().toLowerCase(Locale.ROOT));
    }

    // This method encapsulates api-first logic where the task is NOT backed by Camunda, and the completion logic is
    // driven by completion rules stored with the task in Task Management (originating from POST /tasks).
    private List<TaskResource> getApiFirstCompletableTaskResources(SearchEventAndCase searchEventAndCase,
                                                                   List<RoleAssignment> roleAssignments,
                                                                   PermissionRequirements permissionsRequired) {
        List<TaskResource> candidateTaskResources = taskResourceDao.getApiFirstCompletableTaskResources(
            searchEventAndCase, roleAssignments, permissionsRequired);

        if (CollectionUtils.isEmpty(candidateTaskResources)) {
            return emptyList();
        }

        String eventId = searchEventAndCase.getEventId();
        return candidateTaskResources.stream()
            .filter(taskResource -> hasEventCompletionRule(taskResource, eventId))
            .toList();
    }

    private boolean hasEventCompletionRule(TaskResource taskResource, String eventId) {
        return taskResource.getCompletionRules() != null
            && taskResource.getCompletionRules().containsKey(eventId);
    }

    // This method encapsulates existing logic where the task is backed by Camunda, and the configured DMN drives the
    // completion logic.
    private CamundaCompletableTasks getCamundaCompletableTasks(SearchEventAndCase searchEventAndCase,
                                                               List<RoleAssignment> roleAssignments,
                                                               PermissionRequirements permissionsRequired) {
        List<Map<String, CamundaVariable>> evaluateDmnResult;
        try {
            evaluateDmnResult = camundaService.evaluateTaskCompletionDmn(searchEventAndCase);
        } catch (ServerErrorException ex) {
            if (isDmnNotFound(ex)) {
                log.debug("Completion DMN not found for jurisdiction \"{}\" and case type \"{}\".",
                         searchEventAndCase.getCaseJurisdiction(), searchEventAndCase.getCaseType());
                return CamundaCompletableTasks.empty();
            }
            throw ex;
        }

        List<String> taskTypes = extractTaskTypes(evaluateDmnResult);
        if (taskTypes.isEmpty()) {
            log.info("No taskTypes were found from Completion DMN using eventId: \"{}\" and caseId: \"{}\"",
                     searchEventAndCase.getEventId(), searchEventAndCase.getCaseId());
            return CamundaCompletableTasks.empty();
        }

        List<TaskResource> camundaTaskResources = Optional.ofNullable(taskResourceDao.getCompletableTaskResources(
            searchEventAndCase,
            roleAssignments,
            permissionsRequired,
            taskTypes
        )).orElse(emptyList());

        return new CamundaCompletableTasks(
            isTaskRequired(evaluateDmnResult, taskTypes),
            camundaTaskResources
        );
    }

    private boolean isDmnNotFound(ServerErrorException serverErrorException) {
        Throwable cause = serverErrorException.getCause();
        while (cause != null) {
            if (cause instanceof FeignException feignException && feignException.status() == 404) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isApiFirstTaskRequiredForEvent(List<TaskResource> taskResources,
                                                   SearchEventAndCase searchEventAndCase) {
        String eventId = searchEventAndCase.getEventId();
        return taskResources.stream()
            .map(TaskResource::getCompletionRules)
            .filter(Objects::nonNull)
            .map(rules -> rules.get(eventId))
            .anyMatch(Boolean.TRUE::equals);
    }

    private List<TaskResource> mergeTaskResources(List<TaskResource> camundaTaskResources,
                                                  List<TaskResource> apiFirstTaskResources) {
        Map<String, TaskResource> mergedTaskResources = new LinkedHashMap<>();
        camundaTaskResources.forEach(taskResource ->
                                         mergedTaskResources.put(taskResource.getTaskId(), taskResource));
        apiFirstTaskResources.forEach(taskResource ->
                                          mergedTaskResources.putIfAbsent(taskResource.getTaskId(), taskResource));
        return mergedTaskResources.values().stream()
            .toList();
    }

    private record CamundaCompletableTasks(boolean taskRequiredForEvent, List<TaskResource> taskResources) {
        private static CamundaCompletableTasks empty() {
            return new CamundaCompletableTasks(false, emptyList());
        }
    }

}
