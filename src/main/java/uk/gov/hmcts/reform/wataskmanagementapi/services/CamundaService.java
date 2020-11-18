package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis",
    "PMD.LawOfDemeter", "PMD.AvoidDuplicateLiterals","PMD.TooManyMethods"})
public class CamundaService {

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
        Objects.requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId cannot be null");

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
                String.format("User did not have sufficient permissions to claim task with id: %s", taskId)
            );
        }

    }


    public void assignTask(String taskId, String userId) {
        Map<String, String> body = new ConcurrentHashMap<>();
        body.put("userId", userId);
        HashMap<String, CamundaValue<String>> variable = new HashMap<>();
        variable.put("taskState", CamundaValue.stringValue("assigned"));
        AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);
        try {
            camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), taskId, camundaLocalVariables);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(
                String.format(
                    "There was a problem updating the task with id: %s. The task could not be found.",
                    taskId
                ), ex);
        }
        try {
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
        String userId = accessControlResponse.getUserInfo().getUid();
        Objects.requireNonNull(userId, "UserId must be null");
        CamundaTask camundaTask = performGetCamundaTaskAction(taskId);

        boolean isSameUser = userId.equals(camundaTask.getAssignee());

        if (isSameUser) {
            Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

            boolean hasAccess = permissionEvaluatorService
                .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

            if (hasAccess) {
                String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
                performUnclaimTaskAction(taskState, taskId);
            } else {
                throw new InsufficientPermissionsException(
                    String.format("User did not have sufficient permissions to unclaim task with id: %s", taskId)
                );
            }
        } else {
            throw new InsufficientPermissionsException("Task was not claimed by this user");
        }
    }

    public void completeTask(String taskId,
                             AccessControlResponse accessControlResponse,
                             List<PermissionTypes> permissionsRequired) {
        Objects.requireNonNull(accessControlResponse.getUserInfo().getUid(), "UserId cannot be null");

        Map<String, CamundaVariable> variables = performGetVariablesAction(taskId);

        boolean hasAccess = permissionEvaluatorService
            .hasAccess(variables, accessControlResponse.getRoleAssignments(), permissionsRequired);

        if (hasAccess) {
            // Check that task state was not already completed
            String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
            boolean taskHasCompleted = COMPLETED.value().equals(taskState);

            if (!taskHasCompleted) {
                // If task was not already completed complete it
                performCompleteTaskAction(taskId);
            }
        } else {
            throw new InsufficientPermissionsException(
                String.format("User did not have sufficient permissions to complete task with id: %s", taskId)
            );
        }

    }

    public List<Task> searchWithCriteria(SearchTaskRequest searchTaskRequest,
                                         List<Assignment> roleAssignments,
                                         List<PermissionTypes> permissionsRequired) {

        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);
        return performSearchAction(query, roleAssignments, permissionsRequired);

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
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }
        return variables;
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
            camundaServiceApi.claimTask(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            camundaErrorDecoder.decodeException(ex);
        }
    }

    private List<Task> performSearchAction(CamundaSearchQuery query,
                                           List<Assignment> roleAssignments,
                                           List<PermissionTypes> permissionsRequired) {
        List<Task> response = new ArrayList<>();
        try {
            //1. Perform the search
            List<CamundaTask> searchResults = camundaServiceApi.searchWithCriteria(
                authTokenGenerator.generate(),
                query.getQueries()
            );

            //Loop through all search results
            searchResults.forEach(camundaTask -> {
                //2. Get Variables for the task
                Map<String, CamundaVariable> variables = performGetVariablesAction(camundaTask.getId());

                //3. Evaluate access to task
                boolean hasAccess = permissionEvaluatorService
                    .hasAccess(variables, roleAssignments, permissionsRequired);

                if (hasAccess) {
                    //4. If user had sufficient access to this task map to a task object and add to response
                    Task task = taskMapper.mapToTaskObject(variables, camundaTask);
                    response.add(task);
                }
            });
            return response;
        } catch (FeignException | ResourceNotFoundException ex) {
            throw new ServerErrorException("There was a problem performing the search", ex);
        }
    }

    private void performCompleteTaskAction(String taskId) {
        try {
            //1. Update taskState variable in camunda
            HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
            modifications.put(
                TASK_STATE.value(), CamundaValue.stringValue(COMPLETED.value())
            );

            camundaServiceApi.addLocalVariablesToTask(
                authTokenGenerator.generate(),
                taskId,
                new AddLocalVariableRequest(modifications)
            );

            //2. Call Complete in camunda
            camundaServiceApi.completeTask(authTokenGenerator.generate(), taskId, new CompleteTaskVariables());
        } catch (FeignException ex) {
            throw new ServerErrorException(String.format(
                "There was a problem completing the task with id: %s",
                taskId
            ), ex);
        }
    }

    private void performUnclaimTaskAction(String taskState, String taskId) {
        try {

            if (!UNASSIGNED.value().equals(taskState)) {
                HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
                modifications.put(
                    TASK_STATE.value(), CamundaValue.stringValue(UNASSIGNED.value())
                );

                //1. Add/Update local variables to task
                camundaServiceApi.addLocalVariablesToTask(
                    authTokenGenerator.generate(),
                    taskId,
                    new AddLocalVariableRequest(modifications)
                );
            }

            //2. Call Unclaim in camunda
            camundaServiceApi.unclaimTask(authTokenGenerator.generate(), taskId);
        } catch (FeignException ex) {
            throw new ServerErrorException(String.format(
                "There was a problem unclaiming task: %s",
                taskId
            ), ex);
        }
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }

}
