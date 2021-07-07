package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;

@Slf4j
@Service
public class TaskManagementService {
    public static final String USER_ID_CANNOT_BE_NULL = "UserId cannot be null";

    private final CamundaService camundaService;
    private final CamundaQueryBuilder camundaQueryBuilder;
    private final PermissionEvaluatorService permissionEvaluatorService;

    @Autowired
    public TaskManagementService(CamundaService camundaService,
                                 CamundaQueryBuilder camundaQueryBuilder,
                                 PermissionEvaluatorService permissionEvaluatorService) {
        this.camundaService = camundaService;
        this.camundaQueryBuilder = camundaQueryBuilder;
        this.permissionEvaluatorService = permissionEvaluatorService;
    }

    public Task getTask(String taskId, List<Assignment> roleAssignments) {
        List<PermissionTypes> permissionsRequired = singletonList(READ);
        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
        roleAssignmentVerification(variables, roleAssignments, permissionsRequired);
        return camundaService.getMappedTask(taskId, variables);
    }

    public void claimTask(String taskId,
                          AccessControlResponse accessControlResponse) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
        roleAssignmentVerification(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);
        camundaService.claimTask(taskId, accessControlResponse.getUserInfo().getUid());
    }

    public void unclaimTask(String taskId, AccessControlResponse accessControlResponse) {
        String userId = accessControlResponse.getUserInfo().getUid();
        List<PermissionTypes> permissionsRequired = singletonList(MANAGE);
        CamundaTask camundaTask = camundaService.getUnmappedCamundaTask(taskId);
        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);

        roleAssignmentVerificationWithAssigneeCheckAndHierarchy(
            camundaTask.getAssignee(),
            userId,
            variables,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        camundaService.unclaimTask(taskId, variables);
    }

    public void assignTask(String taskId,
                           AccessControlResponse assignerAccessControlResponse,
                           AccessControlResponse assigneeAccessControlResponse) {
        requireNonNull(assignerAccessControlResponse.getUserInfo().getUid(), "Assigner userId cannot be null");
        requireNonNull(assigneeAccessControlResponse.getUserInfo().getUid(), "Assignee userId cannot be null");
        List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
        List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);

        roleAssignmentVerification(
            variables,
            assignerAccessControlResponse.getRoleAssignments(),
            assignerPermissionsRequired
        );
        roleAssignmentVerification(
            variables,
            assigneeAccessControlResponse.getRoleAssignments(),
            assigneePermissionsRequired
        );

        camundaService.assignTask(taskId, assigneeAccessControlResponse.getUserInfo().getUid(), variables);
    }

    public void cancelTask(String taskId,
                           AccessControlResponse accessControlResponse) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        List<PermissionTypes> permissionsRequired = singletonList(CANCEL);

        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
        roleAssignmentVerification(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        camundaService.cancelTask(taskId);

    }

    public void completeTask(String taskId, AccessControlResponse accessControlResponse) {

        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);

        CamundaTask camundaTask = camundaService.getUnmappedCamundaTask(taskId);

        //Safe-guard
        if (camundaTask.getAssignee() == null) {
            throw new TaskStateIncorrectException(
                String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
            );
        }

        String userId = accessControlResponse.getUserInfo().getUid();

        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);

        roleAssignmentVerificationWithAssigneeCheckAndHierarchy(
            camundaTask.getAssignee(),
            userId,
            variables,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );
        camundaService.completeTask(taskId, variables);
    }

    /**
     * This method is only used by privileged clients allowing them to set a predefined options for completion.
     *
     * @param taskId                The task id to complete.
     * @param accessControlResponse The access control response containing IDAM user info and user role assignments.
     * @param completionOptions     The completion options to orchestrate how this completion should be handled.
     */
    public void completeTaskWithPrivilegeAndCompletionOptions(String taskId,
                                                              AccessControlResponse accessControlResponse,
                                                              CompletionOptions completionOptions) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);

        if (completionOptions.isAssignAndComplete()) {
            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
            roleAssignmentVerification(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

            camundaService.assignAndCompleteTask(taskId, accessControlResponse.getUserInfo().getUid(), variables);
        } else {
            completeTask(taskId, accessControlResponse);
        }
    }


    public List<Task> searchWithCriteria(SearchTaskRequest searchTaskRequest,
                                         int firstResult, int maxResults,
                                         AccessControlResponse accessControlResponse) {
        List<PermissionTypes> permissionsRequired = singletonList(READ);

        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);

        //Safe-guard to avoid sending empty orQueries to camunda and abort early
        if (query == null) {
            return emptyList();
        }

        return camundaService.searchWithCriteria(
            query,
            firstResult,
            maxResults,
            accessControlResponse,
            permissionsRequired
        );
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    public GetTasksCompletableResponse<Task> searchForCompletableTasks(SearchEventAndCase searchEventAndCase,
                                                                       AccessControlResponse accessControlResponse) {

        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);

        //Safe-guard against unsupported Jurisdictions and case types.
        if (!"IA".equalsIgnoreCase(searchEventAndCase.getCaseJurisdiction())
            || !"Asylum".equalsIgnoreCase(searchEventAndCase.getCaseType())) {
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

        //2. Build query and perform search
        CamundaSearchQuery camundaSearchQuery =
            camundaQueryBuilder.createCompletableTasksQuery(searchEventAndCase.getCaseId(), taskTypes);
        //3. Perform the search
        List<CamundaTask> searchResults = camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery);

        //Safe guard in case no search results were returned
        if (searchResults.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }
        //4. Extract if a task is assigned and assignee is idam userId
        String idamUserId = accessControlResponse.getUserInfo().getUid();

        final List<CamundaTask> tasksAssignedToUser = searchResults.stream().filter(
            task -> idamUserId.equals(task.getAssignee()))
            .collect(Collectors.toList());

        if (!tasksAssignedToUser.isEmpty()) {
            searchResults = tasksAssignedToUser;
        }

        final List<Task> taskList = camundaService.performSearchAction(
            searchResults,
            accessControlResponse,
            permissionsRequired
        );

        if (taskList.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

        return new GetTasksCompletableResponse<>(taskRequiredForEvent, taskList);
    }

    public long getTaskCount(SearchTaskRequest searchTaskRequest) {
        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);
        //Safe-guard to avoid sending empty orQueries to camunda and abort early
        if (query == null) {
            return 0;
        }
        return camundaService.getTaskCount(query);
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

    private List<String> extractTaskTypes(List<Map<String, CamundaVariable>> evaluateDmnResult) {
        return evaluateDmnResult.stream()
            .filter(result -> result.containsKey(TASK_TYPE.value()))
            .map(result -> camundaService.getVariableValue(result.get(TASK_TYPE.value()), String.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    }

    /**
     * Helper method to evaluate whether a user should have access to a task.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param variables           the task variables obtained from camunda.
     * @param roleAssignments     the role assignments of the user.
     * @param permissionsRequired the permissions that are required by the endpoint.
     */
    private void roleAssignmentVerification(Map<String, CamundaVariable> variables,
                                            List<Assignment> roleAssignments,
                                            List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = permissionEvaluatorService.hasAccess(variables, roleAssignments, permissionsRequired);
        if (!hasAccess) {
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }
    }

    /**
     * Helper method to evaluate whether a user should have access to a task.
     * This method also performs extra checks for hierarchy and assignee.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param currentAssignee     the IDAM id of the assigned user in the task.
     * @param userId              the IDAM userId of the user making the request.
     * @param variables           the task variables obtained from camunda.
     * @param roleAssignments     the role assignments of the user.
     * @param permissionsRequired the permissions that are required by the endpoint.
     */
    private void roleAssignmentVerificationWithAssigneeCheckAndHierarchy(String currentAssignee,
                                                                         String userId,
                                                                         Map<String, CamundaVariable> variables,
                                                                         List<Assignment> roleAssignments,
                                                                         List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            currentAssignee,
            userId,
            variables,
            roleAssignments,
            permissionsRequired
        );
        if (!hasAccess) {
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }
    }


}
