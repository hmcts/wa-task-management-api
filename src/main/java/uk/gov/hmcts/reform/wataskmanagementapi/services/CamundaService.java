package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
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

    @Autowired
    public CamundaService(CamundaServiceApi camundaServiceApi,
                          CamundaQueryBuilder camundaQueryBuilder,
                          TaskMapper taskMapper,
                          CamundaErrorDecoder camundaErrorDecoder
    ) {
        this.camundaServiceApi = camundaServiceApi;
        this.camundaQueryBuilder = camundaQueryBuilder;
        this.taskMapper = taskMapper;
        this.camundaErrorDecoder = camundaErrorDecoder;
    }

    public CamundaTask getTask(String id) {
        try {
            return camundaServiceApi.getTask(id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem fetching the task with id: %s",
                id
            ), ex);
        }
    }

    public void claimTask(String taskId, String userId) {
        try {
            Map<String, String> body = new ConcurrentHashMap<>();
            body.put("userId", userId);
            camundaServiceApi.claimTask(taskId, body);
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
            camundaServiceApi.addLocalVariablesToTask(taskId, camundaLocalVariables);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(
                String.format("There was a problem updating the task with id: %s. The task could not be found.",
                              taskId
                ), ex);
        }
        try {
            camundaServiceApi.assignTask(taskId, body);
        } catch (FeignException ex) {
            throw new ServerErrorException(
                String.format("There was a problem assigning the task with id: %s",
                taskId
            ), ex);
        }
    }

    public void unclaimTask(String id) {
        try {
            HashMap<String, CamundaValue<String>> variable = new HashMap<>();
            variable.put("taskState", CamundaValue.stringValue("unassigned"));
            AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);
            camundaServiceApi.addLocalVariablesToTask(id, camundaLocalVariables);
            camundaServiceApi.unclaimTask(id);
        } catch (FeignException ex) {
            throw new ResourceNotFoundException(String.format(
                "There was a problem unclaiming the task with id: %s",
                id
            ), ex);
        }
    }

    public void completeTask(String id) {
        try {
            List<HistoryVariableInstance> taskVariables = camundaServiceApi.getTaskVariables(id);

            boolean taskHasCompleted = taskVariables.stream()
                .anyMatch(taskVariable -> taskVariable.getName().equals("taskState")
                                          && taskVariable.getValue().equals("completed"));

            if (!taskHasCompleted) {
                HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
                modifications.put("taskState", CamundaValue.stringValue("completed"));
                camundaServiceApi.addLocalVariablesToTask(id, new AddLocalVariableRequest(modifications));
                camundaServiceApi.completeTask(id, new CompleteTaskVariables());
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
            List<CamundaTask> searchResults = camundaServiceApi.searchWithCriteria(query.getQueries());

            searchResults.forEach(camundaTask -> {
                Map<String, CamundaVariable> variables = camundaServiceApi.getVariables(camundaTask.getId());
                Task task = taskMapper.mapToTaskObject(camundaTask, variables);
                response.add(task);
            });

        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem performing the search", ex);
        }

        return response;

    }
}
