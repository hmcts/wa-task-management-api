package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype.TaskTypeResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.TaskTypesDmnResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.DmnEvaluationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

@Slf4j
@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskTypesService {

    private static final String DMN_NAME = "Task Types DMN";
    private final DmnEvaluationService dmnEvaluationService;

    @Autowired
    public TaskTypesService(DmnEvaluationService dmnEvaluationService) {
        this.dmnEvaluationService = dmnEvaluationService;
    }

    /**
     * Retrieves a task types for a jurisdiction given role assignments.
     *
     * @param accessControlResponse containing the access management roles.
     * @return A mapped optional of work type {@link TaskType}
     */
    public List<TaskTypeResponse> getTaskTypes(AccessControlResponse accessControlResponse, String jurisdiction) {

        //Safe-guard
        if (accessControlResponse.getRoleAssignments().isEmpty()) {
            return emptyList();
        }

        //get task-type-dmn(s) for jurisdiction
        Set<TaskTypesDmnResponse> taskTypesDmnResponse =
            dmnEvaluationService.getTaskTypesDmn(jurisdiction, DMN_NAME);

        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = new ArrayList<>();

        //evaluate dmn(s)
        taskTypesDmnResponse.forEach(response -> taskTypesDmnEvaluationResponses.addAll(
                dmnEvaluationService.evaluateTaskTypesDmn(jurisdiction, response.getKey())
            )
        );

        return extractValues(taskTypesDmnEvaluationResponses);
    }

    private List<TaskTypeResponse> extractValues(List<TaskTypesDmnEvaluationResponse> evaluationResponses) {

        List<TaskTypeResponse> taskTypeResponses = new ArrayList<>();

        evaluationResponses.forEach(item -> {

                TaskType taskType = new TaskType(
                    item.getTaskTypeId().getValue(),
                    item.getTaskTypeName().getValue()
                );

                TaskTypeResponse taskTypeResponse = new TaskTypeResponse(taskType);

                boolean isExist = taskTypeResponses.stream()
                    .anyMatch(t -> t.getTaskType().getTaskTypeId().equalsIgnoreCase(taskType.getTaskTypeId()));

                if (!isExist) {
                    taskTypeResponses.add(taskTypeResponse);
                }

            }
        );

        return taskTypeResponses;

    }

}
