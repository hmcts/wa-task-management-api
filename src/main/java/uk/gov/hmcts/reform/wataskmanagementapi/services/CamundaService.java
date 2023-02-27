package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAlreadyClaimedException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaCftTaskStateUpdateException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskStateUpdateException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaTaskUnclaimException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable.WA_TASK_COMPLETION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.INITIATION_TIMESTAMP;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
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

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.GodClass", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.CyclomaticComplexity", "PMD.PreserveStackTrace"
})
public class CamundaService {

    public static final String THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH = "There was a problem performing the search";

    private static final String ESCALATION_CODE = "wa-esc-cancellation";

    private final CamundaServiceApi camundaServiceApi;
    private final TaskMapper taskMapper;
    private final AuthTokenGenerator authTokenGenerator;
    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi,
                          TaskMapper taskMapper,
                          AuthTokenGenerator authTokenGenerator,
                          CamundaObjectMapper camundaObjectMapper) {
        this.camundaServiceApi = camundaServiceApi;
        this.taskMapper = taskMapper;
        this.authTokenGenerator = authTokenGenerator;
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
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

    public boolean isCftTaskStateExistInCamunda(String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        AtomicBoolean isCftTaskStateExist = new AtomicBoolean(false);

        try {
            //Check if the task has already been deleted or pending termination
            List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(authTokenGenerator.generate(), body);

            if (result == null || result.isEmpty()) {
                return isCftTaskStateExist.get();
            }

            Optional<HistoryVariableInstance> historyVariableInstance = result.stream()
                .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
                .findFirst();

            historyVariableInstance.ifPresent(variable -> {
                log.info("{} cftTaskStateInCamundaHistory: {}", taskId, variable.getValue());
                isCftTaskStateExist.set(true);
            });

            return isCftTaskStateExist.get();

        } catch (FeignException ex) {
            throw new TaskCancelException(TASK_CANCEL_UNABLE_TO_CANCEL);
        }

    }

    public boolean isTaskCompletedInCamunda(String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        AtomicBoolean isTaskStateCompleted = new AtomicBoolean(false);

        //Check if the task has already been deleted or pending termination
        List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(authTokenGenerator.generate(), body);

        if (result == null || result.isEmpty()) {
            return isTaskStateCompleted.get();
        }

        Optional<HistoryVariableInstance> historyVariableInstance = result.stream()
            .filter(r -> r.getName().equals(TASK_STATE.value()))
            .findFirst();

        historyVariableInstance.ifPresent(variable -> {
            if (variable.getValue().equalsIgnoreCase(TaskState.COMPLETED.value())) {
                log.info("{} taskStateInCamundaHistory: {}", taskId, variable.getValue());
                isTaskStateCompleted.set(true);
            }
        });

        return isTaskStateCompleted.get();
    }

    public void cancelTask(String taskId) {

        //Task has not been canceled by dmn, perform the delete action
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

    public void unclaimTask(String taskId, boolean taskHasUnassigned) {

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
                           boolean isTaskStateAssigned) {
        try {
            performAssignTaskAction(
                taskId,
                assigneeUserId,
                isTaskStateAssigned
            );
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskAssignException(TASK_ASSIGN_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskAssignException ex) {
            throw new TaskAssignException(TASK_ASSIGN_UNABLE_TO_ASSIGN);
        }
    }

    public void completeTask(String taskId,
                             boolean taskHasCompleted) {
        try {
            performCompleteTaskAction(taskId, taskHasCompleted);
        } catch (CamundaTaskStateUpdateException ex) {
            throw new TaskCompleteException(TASK_COMPLETE_UNABLE_TO_UPDATE_STATE);
        } catch (CamundaTaskCompleteException ex) {
            throw new TaskCompleteException(TASK_COMPLETE_UNABLE_TO_COMPLETE);
        }
    }

    public void completeTaskById(String taskId) {

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);
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
                                      boolean taskStateIsAssignedAlready
    ) {

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

            List<Map<String, CamundaVariable>> dmnResponse = camundaServiceApi.evaluateDMN(
                authTokenGenerator.generate(),
                taskCompletionDecisionTableKey,
                searchEventAndCase.getCaseJurisdiction().toLowerCase(Locale.ROOT),
                createEventIdDmnRequest(searchEventAndCase.getEventId())
            );

            return dmnResponse.stream().map(CamundaHelper::removeSpaces).collect(Collectors.toList());
        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem evaluating DMN", ex);
        }
    }

    /**
     * Updates the cftTaskState in camunda.
     *
     * @param taskId    the task id
     * @param taskState the new task state
     */
    public void updateCftTaskState(String taskId, TaskState taskState) {
        try {
            updateCftTaskStateTo(taskId, taskState);
        } catch (CamundaCftTaskStateUpdateException ex) {
            throw new ServerErrorException("There was a problem when updating the cftTaskState", ex);
        }
    }

    /**
     * Removes 'cft_task_state' process variable from the history.
     * Since the delete method only offers deletion via variable instance id the history must be retrieved.
     *
     * @param taskId the task id
     */
    public void deleteCftTaskState(String taskId) {

        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        try {
            List<HistoryVariableInstance> results = camundaServiceApi.searchHistory(
                authTokenGenerator.generate(),
                body
            );

            Optional<HistoryVariableInstance> maybeCftTaskState = results.stream()
                .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
                .findFirst();

            maybeCftTaskState.ifPresent(
                historyVariableInstance -> camundaServiceApi.deleteVariableFromHistory(
                    authTokenGenerator.generate(),
                    historyVariableInstance.getId()
                )
            );
        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem when deleting the historic cftTaskState", ex);
        }
    }

    private Map<String, CamundaVariable> performGetVariablesAction(String id) {
        try {
            return camundaServiceApi.getVariables(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            log.error("There was a problem fetching the variables for task with id: {} \n{}", id, ex.getMessage());
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the variables for task with id: %s",
                id
            ));
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
                    throw new TaskAlreadyClaimedException(camundaException.getMessage(), ex);
                case "NullValueException":
                    throw new ResourceNotFoundException(camundaException.getMessage(), ex);
                default:
                    throw new CamundaTaskClaimException(ex);
            }
        }

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
     * Performs cft task state update orchestration in camunda.
     *
     * @throws CamundaTaskStateUpdateException if call fails when updating the cft task state.
     */
    private void updateCftTaskStateTo(String taskId, TaskState newState) {
        String formattedInitiationTime = CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now());

        Map<String, CamundaValue<String>> variable = Map.of(
            CFT_TASK_STATE.value(), CamundaValue.stringValue(newState.value()),
            INITIATION_TIMESTAMP.value(), CamundaValue.stringValue(formattedInitiationTime)
        );
        AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);

        try {
            camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), taskId, camundaLocalVariables);
        } catch (FeignException ex) {
            log.error(
                "There was a problem updating task '{}', cft task state could not be updated to '{}'",
                taskId, newState
            );
            throw new CamundaCftTaskStateUpdateException(ex);
        }

        log.debug("Updated task '{}' with cft task state '{}'", taskId, newState.value());
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
