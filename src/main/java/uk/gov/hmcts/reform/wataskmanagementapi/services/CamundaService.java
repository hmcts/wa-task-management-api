package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.AvoidDuplicateLiterals"})
public class CamundaService {

    private final CamundaServiceApi camundaServiceApi;
    private final CamundaErrorDecoder camundaErrorDecoder;
    private final CamundaQueryBuilder camundaQueryBuilder;
    private final TaskMapper taskMapper;
    private final AuthTokenGenerator authTokenGenerator;
    private final PermissionEvaluatorService permissionEvaluatorService;


    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi,
                          CamundaQueryBuilder camundaQueryBuilder,
                          CamundaErrorDecoder camundaErrorDecoder,
                          TaskMapper taskMapper,
                          AuthTokenGenerator authTokenGenerator,
                          PermissionEvaluatorService permissionEvaluatorService
    ) {
        this.camundaServiceApi = camundaServiceApi;
        this.camundaQueryBuilder = camundaQueryBuilder;
        this.camundaErrorDecoder = camundaErrorDecoder;
        this.taskMapper = taskMapper;
        this.authTokenGenerator = authTokenGenerator;
        this.permissionEvaluatorService = permissionEvaluatorService;
    }


    public void claimTask(String taskId, String userId) {
        try {
            Map<String, String> body = new ConcurrentHashMap<>();
            body.put("userId", userId);
            camundaServiceApi.claimTask(authTokenGenerator.generate(), taskId, body);
        } catch (FeignException ex) {
            if (HttpStatus.NOT_FOUND.value() == ex.status()) {
                throw new ResourceNotFoundException(String.format(
                    "There was a problem claiming the task with id: %s",
                    taskId
                ), ex);
            } else {
                camundaErrorDecoder.decodeException(ex);
            }
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

    public void unclaimTask(String id) {
        try {
            HashMap<String, CamundaValue<String>> variable = new HashMap<>();
            variable.put("taskState", CamundaValue.stringValue(TaskState.UNASSIGNED.getTaskState()));
            AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);
            camundaServiceApi.addLocalVariablesToTask(authTokenGenerator.generate(), id, camundaLocalVariables);
            camundaServiceApi.unclaimTask(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem unclaiming the task with id: %s",
                id
            ), ex);
        }
    }

    public void completeTask(String id) {
        try {
            List<HistoryVariableInstance> taskVariables = camundaServiceApi.getTaskVariables(
                authTokenGenerator.generate(),
                id
            );

            boolean taskHasCompleted = taskVariables.stream()
                .anyMatch(taskVariable -> taskVariable.getName().equals("taskState")
                                          && taskVariable.getValue().equals("completed"));

            if (!taskHasCompleted) {
                HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
                modifications.put("taskState", CamundaValue.stringValue("completed"));
                camundaServiceApi.addLocalVariablesToTask(
                    authTokenGenerator.generate(),
                    id,
                    new AddLocalVariableRequest(modifications)
                );
                camundaServiceApi.completeTask(authTokenGenerator.generate(), id, new CompleteTaskVariables());
            }
        } catch (FeignException ex) {
            // This endpoint throws a 500 when the task doesn't exist.
            throw new ResourceNotFoundException(String.format(
                "There was a problem completing the task with id: %s",
                id
            ), ex);
        }
    }

    public List<Task> searchWithCriteria(SearchTaskRequest searchTaskRequest) {
        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);
        List<Task> response = new ArrayList<>();
        try {
            List<CamundaTask> searchResults = camundaServiceApi.searchWithCriteria(
                authTokenGenerator.generate(),
                query.getQueries()
            );

            searchResults.forEach(camundaTask -> {
                Map<String, CamundaVariable> variables = camundaServiceApi
                    .getVariables(authTokenGenerator.generate(), camundaTask.getId());
                Task task = taskMapper.mapToTaskObject(variables, camundaTask);
                response.add(task);
            });

        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem performing the search", ex);
        }

        return response;

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
        Map<String, CamundaVariable> variables = getVariables(id);

        boolean hasAccess = false;

        // Loop through the roleAssignments and attempt to find a role that s
        for (Assignment roleAssignment : roleAssignments) {
            //Safe-guard
            if (hasAccess) {
                break;
            }
            hasAccess = permissionEvaluatorService.hasAccess(variables, roleAssignment, permissionsRequired);
        }

        if (hasAccess) {
            CamundaTask camundaTask = getCamundaTask(id);
            return taskMapper.mapToTaskObject(variables, camundaTask);
        }

        throw new InsufficientPermissionsException(
            String.format("User did not have sufficient permissions to access task with id: %s", id)
        );
    }

    private Map<String, CamundaVariable> getVariables(String id) {
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

    private CamundaTask getCamundaTask(String id) {
        try {
            return camundaServiceApi.getTask(authTokenGenerator.generate(), id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }
    }
}
