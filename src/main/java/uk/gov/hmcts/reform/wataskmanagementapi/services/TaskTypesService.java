package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskTypesDmnResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype.TaskType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype.TaskTypeResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomizedConstraintViolationException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

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
    public GetTaskTypesResponse getTaskTypes(AccessControlResponse accessControlResponse, String jurisdiction) {

        validateRequest(jurisdiction);

        GetTaskTypesResponse getTaskTypesResponse = new GetTaskTypesResponse();
        //Safe-guard
        if (accessControlResponse.getRoleAssignments().isEmpty()) {
            return getTaskTypesResponse;
        }

        //get task-type-dmn(s) for jurisdiction
        Set<TaskTypesDmnResponse> taskTypesDmnResponse =
            dmnEvaluationService.retrieveTaskTypesDmn(jurisdiction, DMN_NAME);

        List<TaskTypesDmnEvaluationResponse> taskTypesDmnEvaluationResponses = new ArrayList<>();

        //evaluate dmn(s)
        taskTypesDmnResponse.forEach(response -> taskTypesDmnEvaluationResponses.addAll(
                dmnEvaluationService.evaluateTaskTypesDmn(jurisdiction, response.getKey())
            )
        );

        List<TaskTypeResponse> taskTypeResponses = extractValues(taskTypesDmnEvaluationResponses);

        if (!taskTypeResponses.isEmpty()) {
            getTaskTypesResponse = new GetTaskTypesResponse(taskTypeResponses);
        }

        return getTaskTypesResponse;
    }

    private List<TaskTypeResponse> extractValues(List<TaskTypesDmnEvaluationResponse> evaluationResponses) {

        Set<TaskType> taskTypeResponses = new LinkedHashSet<>();

        evaluationResponses.forEach(item -> {

                TaskType taskType = new TaskType(
                    item.getTaskTypeId().getValue(),
                    item.getTaskTypeName().getValue()
                );

                taskTypeResponses.add(taskType);
            }
        );

        return taskTypeResponses.stream().map(TaskTypeResponse::new).collect(Collectors.toList());

    }

    private void validateRequest(String jurisdiction) {

        if (jurisdiction.isEmpty()) {
            Violation violation = new Violation(
                "jurisdiction",
                "A jurisdiction parameter key and value is required."
            );
            throw new CustomizedConstraintViolationException(singletonList(violation));
        }
    }

}
