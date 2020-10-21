package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;

import java.util.Map;


@SuppressWarnings("PMD.UnnecessaryConstructor")
@Service
public class CreateHmctsTaskVariable {


    public CreateHmctsTaskVariable() {
    //Default constructor
    }

    public Task mapToTaskObject(Map<String, CamundaVariable> localVariableResponse, CamundaTask camundaTask) {
        // Camunda Attributes
        String id = camundaTask.getId();
        String name = camundaTask.getName();
        String task = camundaTask.getFormKey();
        String createdDate = camundaTask.getCreated().toString();
        String dueDate = camundaTask.getDue().toString();
        String assignee = camundaTask.getAssignee();
        // Local Variables
        String taskState = getValueFromObject(localVariableResponse, "taskState");
        String securityClassification = getValueFromObject(localVariableResponse, "securityClassification");
        String taskTitle = getValueFromObject(localVariableResponse, "title");
        String executionType = getValueFromObject(localVariableResponse, "executionType");
        Boolean autoAssigned = false;
        String taskSystem = getValueFromObject(localVariableResponse, "taskSystem");
        String jurisdiction = getValueFromObject(localVariableResponse, "jurisdiction");
        String region = getValueFromObject(localVariableResponse, "region");
        String location = getValueFromObject(localVariableResponse, "location");
        String locationName = getValueFromObject(localVariableResponse, "locationName");
        String caseTypeId = getValueFromObject(localVariableResponse, "caseTypeId");
        String caseId = getValueFromObject(localVariableResponse, "ccdId");
        String caseName = getValueFromObject(localVariableResponse, "caseName");
        String caseCategory = getValueFromObject(localVariableResponse, "appealType");

        return new Task(
            id,
            name,
            task,
            taskState,
            taskSystem,
            securityClassification,
            taskTitle,
            createdDate,
            dueDate,
            assignee,
            autoAssigned,
            executionType,
            jurisdiction,
            region,
            location,
            locationName,
            caseTypeId,
            caseId,
            caseCategory,
            caseName

        );
    }

    private String getValueFromObject(Map<String, CamundaVariable> localVariableResponse, String key) {
        return localVariableResponse.entrySet().stream()
            .filter(e -> key.equals(e.getKey()))
            .map(i -> i.getValue().getValue())
            .findFirst()
            .orElse(null);
    }
}
