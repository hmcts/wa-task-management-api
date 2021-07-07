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
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;
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
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskStateUpdateException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskUnclaimException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_COMPLETION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_UPDATE_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_ASSIGN_UNABLE_TO_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_ASSIGN_UNABLE_TO_UPDATE_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_CANCEL_UNABLE_TO_CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_CLAIM_UNABLE_TO_CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_CLAIM_UNABLE_TO_UPDATE_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_COMPLETE_UNABLE_TO_COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_COMPLETE_UNABLE_TO_UPDATE_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_UNCLAIM_UNABLE_TO_UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_UNCLAIM_UNABLE_TO_UPDATE_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder.WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.GodClass", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.CyclomaticComplexity", "PMD.PreserveStackTrace"
})
public class CamundaService {

    public static final String THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH = "There was a problem performing the search";
    public static final String THERE_WAS_A_PROBLEM_RETRIEVING_TASK_COUNT = "There was a problem retrieving task count";

    private static final String ESCALATION_CODE = "wa-esc-cancellation";

    private final CamundaServiceApi camundaServiceApi;
    private final TaskMapper taskMapper;
    private final AuthTokenGenerator authTokenGenerator;
    private final PermissionEvaluatorService permissionEvaluatorService;
    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi,
                          TaskMapper taskMapper,
                          AuthTokenGenerator authTokenGenerator,
                          PermissionEvaluatorService permissionEvaluatorService,
                          CamundaObjectMapper camundaObjectMapper
    ) {
        this.camundaServiceApi = camundaServiceApi;
        this.taskMapper = taskMapper;
        this.authTokenGenerator = authTokenGenerator;
        this.permissionEvaluatorService = permissionEvaluatorService;
        this.camundaObjectMapper = camundaObjectMapper;
    }

    protected <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }


    public Task getMappedTask(String id, Map<String, CamundaVariable> variables) {
        CamundaTask camundaTask = performGetCamundaTaskAction(id);
        return taskMapper.mapToTaskObject(variables, camundaTask);
    }

    public CamundaTask getUnmappedCamundaTask(String taskId) {
        return performGetCamundaTaskAction(taskId);
    }

    public Map<String, CamundaVariable> getTaskVariables(String taskId) {
        return performGetVariablesAction(taskId);
    }

    public void cancelTask(String taskId) {
        try {
            performCancelTaskAction(taskId);
        } catch (CamundaTaskCancelException ex) {
            throw new TaskCancelException(TASK_CANCEL_UNABLE_TO_CANCEL);
        }
    }

    public void claimTask(String taskId, String userId) {
        try {
            performClaimTaskAction(
                taskId,
                Map.of("userId", userId)
            );
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskClaimException(TASK_CLAIM_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskClaimException ex) {
            throw new TaskClaimException(TASK_CLAIM_UNABLE_TO_CLAIM);
        }
    }

    public void unclaimTask(String taskId,
                            Map<String, CamundaVariable> variables) {
        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        boolean taskHasUnassigned = TaskState.UNASSIGNED.value().equals(taskState);

        try {
            performUnclaimTaskAction(taskId, taskHasUnassigned);
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskUnclaimException(TASK_UNCLAIM_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskUnclaimException ex) {
            throw new TaskUnclaimException(TASK_UNCLAIM_UNABLE_TO_UNCLAIM);
        }
    }

    public void assignTask(String taskId,
                           String assigneeUserId,
                           Map<String, CamundaVariable> variables) {

        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        boolean taskStateIsAssignedAlready = TaskState.ASSIGNED.value().equals(taskState);

        try {
            performAssignTaskAction(
                taskId,
                assigneeUserId,
                taskStateIsAssignedAlready
            );
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskAssignException(TASK_ASSIGN_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskAssignException ex) {
            throw new TaskAssignException(TASK_ASSIGN_UNABLE_TO_ASSIGN);
        }
    }

    public void completeTask(String taskId,
                             Map<String, CamundaVariable> variables) {
        // Check that task state was not already completed
        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        boolean taskHasCompleted = TaskState.COMPLETED.value().equals(taskState);

        try {
            performCompleteTaskAction(taskId, taskHasCompleted);
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskCompleteException(TASK_COMPLETE_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskCompleteException ex) {
            throw new TaskCompleteException(TASK_COMPLETE_UNABLE_TO_COMPLETE);
        }

    }

    public void assignAndCompleteTask(String taskId,
                                      String userId,
                                      Map<String, CamundaVariable> variables
    ) {
        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        boolean taskStateIsAssignedAlready = TaskState.ASSIGNED.value().equals(taskState);
        try {
            performAssignTaskAction(
                taskId,
                userId,
                taskStateIsAssignedAlready
            );
            performCompleteTaskAction(taskId, false);
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskAssignAndCompleteException(TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskAssignException ex) {
            throw new TaskAssignAndCompleteException(TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_ASSIGN);
        } catch (CamundaTaskCompleteException ex) {
            throw new TaskAssignAndCompleteException(TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_COMPLETE);
        }

    }

    public long getTaskCount(CamundaSearchQuery query) {
        try {
            return camundaServiceApi.getTaskCount(
                authTokenGenerator.generate(),
                query.getQueries()
            ).getCount();
        } catch (FeignException exp) {
            log.error(THERE_WAS_A_PROBLEM_RETRIEVING_TASK_COUNT);
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_RETRIEVING_TASK_COUNT, exp);
        }
    }

    public List<Task> searchWithCriteria(CamundaSearchQuery query,
                                         int firstResult,
                                         int maxResults,
                                         AccessControlResponse accessControlResponse,
                                         List<PermissionTypes> permissionsRequired) {

        try {
            List<CamundaTask> searchResults = camundaServiceApi.searchWithCriteriaAndPagination(
                authTokenGenerator.generate(),
                firstResult,
                maxResults,
                query.getQueries()
            );

            //Safe guard in case no search results were returned
            if (searchResults.isEmpty()) {
                return emptyList();
            }
            return performSearchAction(searchResults, accessControlResponse, permissionsRequired);
        } catch (FeignException exp) {
            log.error(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH);
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH, exp);
        }
    }

    public List<CamundaTask> searchWithCriteriaAndNoPagination(CamundaSearchQuery query) {
        try {
            return camundaServiceApi.searchWithCriteriaAndNoPagination(
                authTokenGenerator.generate(),
                query.getQueries()
            );
        } catch (FeignException exp) {
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH, exp);
        }
    }

    public CamundaTask performGetCamundaTaskAction(String id) {
        try {
            return camundaServiceApi.getTask(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }
    }

    public List<Map<String, CamundaVariable>> evaluateTaskCompletionDmn(SearchEventAndCase searchEventAndCase) {
        try {

            String taskCompletionDecisionTableKey = WA_TASK_COMPLETION.getTableKey(
                searchEventAndCase.getCaseJurisdiction(),
                searchEventAndCase.getCaseType()
            );

            return camundaServiceApi.evaluateDMN(
                authTokenGenerator.generate(),
                taskCompletionDecisionTableKey,
                createEventIdDmnRequest(searchEventAndCase.getEventId())
            );
        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem evaluating DMN", ex);
        }
    }

    public List<Task> performSearchAction(List<CamundaTask> searchResults,
                                          AccessControlResponse accessControlResponse,
                                          List<PermissionTypes> permissionsRequired) {

        List<Task> response = new ArrayList<>();
        try {

            List<String> processInstanceIdList = searchResults.stream()
                .map(CamundaTask::getProcessInstanceId)
                .collect(Collectors.toList());

            List<CamundaVariableInstance> allVariablesForProcessInstanceIdList =
                retrieveAllVariablesForProcessInstanceList(processInstanceIdList);

            //Safe guard in case no variables where returned
            if (allVariablesForProcessInstanceIdList.isEmpty()) {
                return response;
            }

            Map<String, List<CamundaVariableInstance>> mapWarningVarAndLocalTaskVarsGroupByProcessInstanceId =
                allVariablesForProcessInstanceIdList.stream()
                    .filter(this::filterOnlyHasWarningVarAndLocalTaskVars)
                    .collect(groupingBy(CamundaVariableInstance::getProcessInstanceId));

            loopThroughAllSearchResultsAndBuildResponse(
                searchResults,
                accessControlResponse,
                permissionsRequired,
                response,
                mapWarningVarAndLocalTaskVarsGroupByProcessInstanceId
            );

            return response;
        } catch (FeignException | ResourceNotFoundException ex) {
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH, ex);
        }
    }

    private Map<String, CamundaVariable> performGetVariablesAction(String id) {
        try {
            return camundaServiceApi.getVariables(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the variables for task with id: %s",
                id
            ), ex);
        }
    }

    private Map<String, Map<String, CamundaVariable>> createEventIdDmnRequest(String eventId) {
        requireNonNull(eventId, "eventId cannot be null");

        Map<String, CamundaVariable> eventIdCamundaVariable =
            Map.of("eventId", new CamundaVariable(eventId, "String"));

        return Map.of("variables", eventIdCamundaVariable);
    }

    /**
     * Performs task assign orchestration in camunda updates task state and then proceeds to assign the task.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the task state.
     * @throws CamundaTaskAssignException      if call fails while assigning the task.
     */
    private void performAssignTaskAction(String taskId, String userId, boolean taskStateIsAssignedAlready) {
        Map<String, String> body = new ConcurrentHashMap<>();
        body.put("userId", userId);

        if (!taskStateIsAssignedAlready) {
            updateTaskStateTo(taskId, TaskState.ASSIGNED);
        }

        try {
            camundaServiceApi.assignTask(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            throw new CamundaTaskAssignException(ex);
        }
    }

    /**
     * Performs task claim orchestration in camunda updates task state and then proceeds to claim the task.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the task state.
     * @throws CamundaTaskClaimException       if call fails while claiming the task.
     */
    private void performClaimTaskAction(String taskId, Map<String, String> body) {
        updateTaskStateTo(taskId, TaskState.ASSIGNED);
        try {
            camundaServiceApi.claimTask(authTokenGenerator.generate(), taskId, body);
            log.debug("Task id '{}' successfully claimed", taskId);
        } catch (FeignException ex) {
            CamundaExceptionMessage camundaException =
                camundaObjectMapper.readValue(ex.contentUTF8(), CamundaExceptionMessage.class);

            if (camundaException == null) {
                throw new CamundaTaskClaimException(ex);
            }

            switch (camundaException.getType()) {
                case "TaskAlreadyClaimedException":
                    throw new ConflictException(camundaException.getMessage(), ex);
                case "NullValueException":
                    throw new ResourceNotFoundException(camundaException.getMessage(), ex);
                default:
                    throw new CamundaTaskClaimException(ex);
            }
        }

    }

    private void loopThroughAllSearchResultsAndBuildResponse(
        List<CamundaTask> searchResults,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired,
        List<Task> response,
        Map<String, List<CamundaVariableInstance>> warningVarAndLocalTaskVarsGroupByProcessInstanceId) {

        searchResults.forEach(camundaTask -> {

            //2. Get Variables for the task
            List<CamundaVariableInstance> variablesForProcessInstanceId =
                warningVarAndLocalTaskVarsGroupByProcessInstanceId.get(camundaTask.getProcessInstanceId());
            if (variablesForProcessInstanceId != null) {
                //Format variables
                Map<String, CamundaVariable> variables = variablesForProcessInstanceId.stream()
                    .collect(
                        toMap(
                            CamundaVariableInstance::getName,
                            variable -> new CamundaVariable(variable.getValue(), variable.getType()), (a, b) -> b
                        )
                    );

                //3. Evaluate access to task
                boolean hasAccess = permissionEvaluatorService
                    .hasAccess(
                        variables,
                        accessControlResponse.getRoleAssignments(),
                        permissionsRequired
                    );

                if (hasAccess) {
                    //4. If user had sufficient access to this task map to a task object and add to response
                    Task task = taskMapper.mapToTaskObject(variables, camundaTask);
                    response.add(task);
                }

            }
        });
    }

    private boolean filterOnlyHasWarningVarAndLocalTaskVars(CamundaVariableInstance camundaVariableInstance) {
        if (camundaVariableInstance.getName().equals("hasWarnings") && camundaVariableInstance.getTaskId() == null) {
            return true;
        }
        return camundaVariableInstance.getTaskId() != null;
    }

    private List<CamundaVariableInstance> retrieveAllVariablesForProcessInstanceList(
        List<String> processInstanceIdList) {
        Map<String, Object> body = Map.of(
            "processInstanceIdIn", processInstanceIdList,
            "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
        );

        return camundaServiceApi.getAllVariables(authTokenGenerator.generate(), body);
    }

    /**
     * Performs task completion orchestration in camunda updates task state and then proceeds to complete the task.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the task state.
     * @throws CamundaTaskCompleteException    if call fails while completing the task.
     */
    private void performCompleteTaskAction(String taskId, boolean taskHasCompleted) {

        if (!taskHasCompleted) {
            // If task was not already completed complete it
            updateTaskStateTo(taskId, TaskState.COMPLETED);
        }

        try {
            camundaServiceApi.completeTask(authTokenGenerator.generate(), taskId, new CompleteTaskVariables());
            log.debug("Task '{}' completed", taskId);
        } catch (FeignException ex) {
            log.error("There was a problem completing the task '{}'", taskId);
            throw new CamundaTaskCompleteException(ex);
        }
    }

    /**
     * Performs task unclaim orchestration in camunda updates task state and then proceeds to unclaim the task.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the task state.
     * @throws CamundaTaskUnclaimException     if call fails while unclaiming the task.
     */
    private void performUnclaimTaskAction(String taskId, boolean taskHasUnassigned) {

        if (!taskHasUnassigned) {
            updateTaskStateTo(taskId, TaskState.UNASSIGNED);
        }
        try {
            camundaServiceApi.unclaimTask(authTokenGenerator.generate(), taskId);
            log.debug("Task id '{}' unclaimed", taskId);
        } catch (FeignException ex) {
            log.error("There was a problem while claiming task id '{}'", taskId);
            throw new CamundaTaskUnclaimException(ex);
        }
    }

    /**
     * Performs task state update orchestration in camunda.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the task state.
     */
    private void updateTaskStateTo(String taskId, TaskState newState) {
        Map<String, CamundaValue<String>> variable = Map.of(
            CamundaVariableDefinition.TASK_STATE.value(), CamundaValue.stringValue(newState.value())
        );
        AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);

        try {
            camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), taskId, camundaLocalVariables);
        } catch (FeignException ex) {
            log.error(
                "There was a problem updating task '{}', task state could not be updated to '{}'",
                taskId, newState
            );
            throw new CamundaTaskStateUpdateException(ex);
        }

        log.debug("Updated task '{}' with state '{}'", taskId, newState.value());
    }

    /**
     * Performs task cancellation orchestration in camunda.
     *
     * @throws CamundaTaskCancelException if call fails while cancelling the task.
     */
    private void performCancelTaskAction(String taskId) {
        Map<String, String> body = new ConcurrentHashMap<>();
        body.put("escalationCode", ESCALATION_CODE);
        try {
            camundaServiceApi.bpmnEscalation(authTokenGenerator.generate(), taskId, body);
            log.debug("Task id '{}' cancelled", taskId);
        } catch (FeignException ex) {
            log.error("Task id '{}' could not be cancelled", taskId);
            throw new CamundaTaskCancelException(ex);
        }
    }
}
