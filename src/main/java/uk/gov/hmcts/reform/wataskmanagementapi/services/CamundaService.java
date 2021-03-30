package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.GodClass", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap"
})
public class CamundaService {

    public static final String USER_DID_NOT_HAVE_SUFFICIENT_PERMISSIONS_TO_ASSIGN_TASK =
        "User did not have sufficient permissions to assign task with id: %s";
    public static final String USER_DID_NOT_HAVE_SUFFICIENT_PERMISSIONS_TO_CLAIM_TASK =
        "User did not have sufficient permissions to claim task with id: %s";
    public static final String WA_TASK_COMPLETION_TABLE_NAME = "wa-task-completion";

    private static final String ESCALATION_CODE = "wa-esc-cancellation";

    private final CamundaServiceApi camundaServiceApi;
    private final CamundaErrorDecoder camundaErrorDecoder;
    private final CamundaQueryBuilder camundaQueryBuilder;
    private final TaskMapper taskMapper;
    private final AuthTokenGenerator authTokenGenerator;
    private final PermissionEvaluatorService permissionEvaluatorService;
    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi,
                          CamundaQueryBuilder camundaQueryBuilder,
                          CamundaErrorDecoder camundaErrorDecoder,
                          TaskMapper taskMapper,
                          AuthTokenGenerator authTokenGenerator,
                          PermissionEvaluatorService permissionEvaluatorService,
                          CamundaObjectMapper camundaObjectMapper
    ) {
        this.camundaServiceApi = camundaServiceApi;
        this.camundaQueryBuilder = camundaQueryBuilder;
        this.camundaErrorDecoder = camundaErrorDecoder;
        this.taskMapper = taskMapper;
        this.authTokenGenerator = authTokenGenerator;
        this.permissionEvaluatorService = permissionEvaluatorService;
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public void claimTask(String taskId,
                          AccessControlResponse accessControlResponse,
                          List<PermissionTypes> permissionsRequired) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId cannot be null");

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        boolean hasAccess = permissionEvaluatorService
            .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        if (hasAccess) {
            performClaimTaskAction(
                taskId,
                Map.of("userId", accessControlResponse.getUserInfo().getUid())
            );
        } else {
            throw new InsufficientPermissionsException(
                String.format(USER_DID_NOT_HAVE_SUFFICIENT_PERMISSIONS_TO_CLAIM_TASK, taskId)
            );
        }

    }

    public void assignTask(String taskId,
                           AccessControlResponse assignerAccessControlResponse,
                           List<PermissionTypes> assignerPermissionsRequired,
                           AccessControlResponse assigneeAccessControlResponse,
                           List<PermissionTypes> assigneePermissionsRequired) {
        requireNonNull(assigneeAccessControlResponse.getUserInfo().getUid(), "AssigneeId cannot be null");

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        hasAccess(taskId, assignerAccessControlResponse, assignerPermissionsRequired, variables);
        hasAccess(taskId, assigneeAccessControlResponse, assigneePermissionsRequired, variables);

        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        boolean taskStateIsAssignedAlready = TaskState.ASSIGNED.value().equals(taskState);

        performAssignTaskAction(
            taskId,
            assigneeAccessControlResponse.getUserInfo().getUid(),
            taskStateIsAssignedAlready
        );
    }

    private void hasAccess(String taskId,
                           AccessControlResponse accessControlResponse,
                           List<PermissionTypes> permissionsRequired,
                           Map<String, CamundaVariable> variables) {
        boolean hasAccess = permissionEvaluatorService.hasAccess(
            variables,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );

        if (!hasAccess) {
            throw new InsufficientPermissionsException(
                String.format(USER_DID_NOT_HAVE_SUFFICIENT_PERMISSIONS_TO_ASSIGN_TASK, taskId)
            );
        }
    }

    private void performAssignTaskAction(String taskId,
                                         String userId,
                                         boolean taskStateIsAssignedAlready) {
        Map<String, String> body = new ConcurrentHashMap<>();
        body.put("userId", userId);
        try {
            if (!taskStateIsAssignedAlready) {
                updateTaskStateTo(taskId, TaskState.ASSIGNED);
            }
            camundaServiceApi.assignTask(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            throw new ServerErrorException(
                String.format(
                    "There was a problem assigning the task with id: %s",
                    taskId
                ), ex);
        }
    }

    public void unclaimTask(String taskId,
                            AccessControlResponse accessControlResponse,
                            List<PermissionTypes> permissionsRequired) {
        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        boolean hasAccess = permissionEvaluatorService
            .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        if (hasAccess) {
            String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
            boolean taskHasUnassigned = TaskState.UNASSIGNED.value().equals(taskState);
            performUnclaimTaskAction(taskId, taskHasUnassigned);
        } else {
            throw new InsufficientPermissionsException(
                String.format("User did not have sufficient permissions to unclaim task with id: %s", taskId)
            );
        }
    }

    public void completeTask(String taskId,
                             AccessControlResponse accessControlResponse,
                             List<PermissionTypes> permissionsRequired) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId cannot be null");

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        boolean hasAccess = permissionEvaluatorService
            .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        if (hasAccess) {
            // Check that task state was not already completed
            String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
            boolean taskHasCompleted = TaskState.COMPLETED.value().equals(taskState);

            performCompleteTaskAction(taskId, taskHasCompleted);
        } else {
            throw new InsufficientPermissionsException(
                String.format("User did not have sufficient permissions to complete task with id: %s", taskId)
            );
        }

    }

    public List<Task> searchWithCriteria(SearchTaskRequest searchTaskRequest,
                                         AccessControlResponse accessControlResponse,
                                         List<PermissionTypes> permissionsRequired) {

        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);

        //Safe-guard to avoid sending empty orQueries to camunda and abort early
        if (query == null) {
            return emptyList();
        }

        return performSearchAction(query, accessControlResponse, permissionsRequired);

    }

    public List<Task> searchForCompletableTasks(SearchEventAndCase searchEventAndCase,
                                                List<PermissionTypes> permissionsRequired,
                                                AccessControlResponse accessControlResponse) {

        try {
            //Safe-guard against unsupported Jurisdictions and case types.
            if (!"IA".equalsIgnoreCase(searchEventAndCase.getCaseJurisdiction())
                || !"Asylum".equalsIgnoreCase(searchEventAndCase.getCaseType())) {
                throw new BadRequestException("Please check your request. "
                                              + "This endpoint currently only supports"
                                              + " the Immigration & Asylum service");
            }
            //1. Perform the DMN evaluation
            List<Map<String, CamundaVariable>> evaluateDmnResult =
                camundaServiceApi.evaluateDMN(
                    authTokenGenerator.generate(),
                    getTableKey(searchEventAndCase.getCaseJurisdiction(), searchEventAndCase.getCaseType()),
                    createEventIdDmnRequest(searchEventAndCase.getEventId())
                );

            // Collect task types
            List<String> taskTypes = evaluateDmnResult.stream()
                .map(result -> getVariableValue(result.get("task_type"), String.class))
                .collect(Collectors.toList());


            if (taskTypes.isEmpty()) {
                return emptyList();
            } else {
                //2. Build query and perform search
                CamundaSearchQuery camundaSearchQuery =
                    camundaQueryBuilder.createCompletableTasksQuery(
                        searchEventAndCase.getCaseId(),
                        taskTypes
                    );

                return performSearchAction(
                    camundaSearchQuery,
                    accessControlResponse,
                    permissionsRequired
                );
            }
        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem evaluating DMN", ex);
        }

    }

    private Map<String, Map<String, CamundaVariable>> createEventIdDmnRequest(String eventId) {
        requireNonNull(eventId, "eventId cannot be null");

        Map<String, CamundaVariable> eventIdCamundaVariable =
            Map.of("eventId", new CamundaVariable(eventId, "String"));

        return Map.of("variables", eventIdCamundaVariable);
    }

    private String getTableKey(String jurisdictionId, String caseTypeId) {
        return WA_TASK_COMPLETION_TABLE_NAME + "-" + jurisdictionId.toLowerCase(Locale.getDefault())
               + "-" + caseTypeId.toLowerCase(Locale.getDefault());
    }

    public Task getTask(String id,
                        List<Assignment> roleAssignments,
                        List<PermissionTypes> permissionsRequired) {
        /*
         * Optimizations: This method retrieves the variables first and assesses them
         * if the user has enough permission on the task we will make a second call to camunda
         * to retrieve the remaining variables.
         * Performing the check this way saves an extra call to camunda in cases where
         * the user did not have sufficient permissions
         */

        Map<String, CamundaVariable> variables = performGetVariablesAction(id);

        boolean hasAccess = permissionEvaluatorService.hasAccess(variables, roleAssignments, permissionsRequired);

        if (hasAccess) {
            CamundaTask camundaTask = performGetCamundaTaskAction(id);
            return taskMapper.mapToTaskObject(variables, camundaTask);
        }

        throw new InsufficientPermissionsException(
            String.format("User did not have sufficient permissions to access task with id: %s", id)
        );
    }

    public Map<String, CamundaVariable> performGetVariablesAction(String id) {
        Map<String, CamundaVariable> variables;
        try {
            variables = camundaServiceApi.getVariables(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the variables for task with id: %s",
                id
            ), ex);
        }
        return variables;
    }

    public void cancelTask(String taskId,
                           AccessControlResponse accessControlResponse,
                           List<PermissionTypes> permissionsRequired) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId cannot be null");

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        boolean hasAccess = permissionEvaluatorService.hasAccess(
            variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        if (hasAccess) {
            performCancelTaskAction(taskId);
        } else {
            throw new InsufficientPermissionsException(
                String.format("User did not have sufficient permissions to cancel task with id: %s", taskId)
            );
        }

    }

    private CamundaTask performGetCamundaTaskAction(String id) {
        try {
            return camundaServiceApi.getTask(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }
    }

    private void performClaimTaskAction(String taskId, Map<String, String> body) {
        try {
            updateTaskStateTo(taskId, TaskState.ASSIGNED);
            camundaServiceApi.claimTask(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            camundaErrorDecoder.decodeException(ex);
        }
    }

    private List<Task> performSearchAction(CamundaSearchQuery query,
                                           AccessControlResponse accessControlResponse,
                                           List<PermissionTypes> permissionsRequired) {


        List<Task> response = new ArrayList<>();
        try {
            //1. Perform the search
            List<CamundaTask> searchResults = camundaServiceApi.searchWithCriteria(
                authTokenGenerator.generate(),
                query.getQueries()
            );

            //Safe guard in case no search results were returned
            if (searchResults.isEmpty()) {
                return response;
            }

            //Extract all processIds to be used as a lookup when collecting all variables
            List<String> searchResultsProcessIds = searchResults.stream()
                .map(CamundaTask::getProcessInstanceId)
                .collect(Collectors.toList());

            //Retrieve all variables for processIds
            Map<String, List<String>> body = Map.of("variableScopeIdIn", searchResultsProcessIds);
            List<CamundaVariableInstance> allVariables =
                camundaServiceApi.getAllVariables(authTokenGenerator.generate(), body);

            //Safe guard in case no variables where returned
            if (allVariables.isEmpty()) {
                return response;
            }

            Map<String, List<CamundaVariableInstance>> variablesByProcessId = allVariables.stream()
                .collect(groupingBy(CamundaVariableInstance::getProcessInstanceId));

            String userId = accessControlResponse.getUserInfo().getUid();

            //Loop through all search results
            searchResults.forEach(camundaTask -> {

                //2. Get Variables for the task
                List<CamundaVariableInstance> variablesForProcessId =
                    variablesByProcessId.get(camundaTask.getProcessInstanceId());
                if (variablesForProcessId != null) {
                    //Format variables
                    Map<String, CamundaVariable> variables = variablesForProcessId.stream()
                        .collect(toMap(
                            CamundaVariableInstance::getName,
                            var -> new CamundaVariable(var.getValue(), var.getType()), (a, b) -> b)
                        );

                    //Safe-guard if task is assigned to same user should have access
                    if (camundaTask.getAssignee() != null && camundaTask.getAssignee().equals(userId)) {
                        Task task = taskMapper.mapToTaskObject(variables, camundaTask);
                        response.add(task);
                        return;
                    }

                    //3. Evaluate access to task
                    boolean hasAccess = permissionEvaluatorService
                        .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

                    if (hasAccess) {
                        //4. If user had sufficient access to this task map to a task object and add to response
                        Task task = taskMapper.mapToTaskObject(variables, camundaTask);
                        response.add(task);
                    }
                }
            });

            return response;
        } catch (FeignException | ResourceNotFoundException ex) {
            throw new ServerErrorException("There was a problem performing the search", ex);
        }
    }

    private void performCompleteTaskAction(String taskId, boolean taskHasCompleted) {
        try {
            if (!taskHasCompleted) {
                // If task was not already completed complete it
                updateTaskStateTo(taskId, TaskState.COMPLETED);
            }
            camundaServiceApi.completeTask(authTokenGenerator.generate(), taskId, new CompleteTaskVariables());
        } catch (FeignException ex) {
            throw new ServerErrorException(String.format(
                "There was a problem completing the task with id: %s",
                taskId
            ), ex);
        }
    }

    private void performUnclaimTaskAction(String taskId, boolean taskHasUnassigned) {
        try {

            if (!taskHasUnassigned) {
                // If task was not already completed complete it
                updateTaskStateTo(taskId, TaskState.UNASSIGNED);
            }

            camundaServiceApi.unclaimTask(authTokenGenerator.generate(), taskId);
        } catch (FeignException ex) {
            throw new ServerErrorException(String.format(
                "There was a problem unclaiming task: %s",
                taskId
            ), ex);
        }
    }

    private void updateTaskStateTo(String taskId, TaskState newState) {
        Map<String, CamundaValue<String>> variable = Map.of(
            CamundaVariableDefinition.TASK_STATE.value(), CamundaValue.stringValue(newState.value())
        );
        AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);
        camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), taskId, camundaLocalVariables);
    }

    private void performCancelTaskAction(String taskId) {
        Map<String, String> body = new ConcurrentHashMap<>();
        body.put("escalationCode", ESCALATION_CODE);
        try {
            camundaServiceApi.bpmnEscalation(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            camundaErrorDecoder.decodeException(ex);
        }
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
