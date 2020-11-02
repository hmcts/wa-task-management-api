package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;

import java.util.Map;
import java.util.Optional;


@SuppressWarnings("PMD.UnnecessaryConstructor")
@Service
public class TaskMapper {


    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public TaskMapper(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
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
        String taskState = getValueFromObject(localVariableResponse.get("taskState"), String.class);
        String securityClassification = getValueFromObject(localVariableResponse
                                                               .get("securityClassification"), String.class);
        String taskTitle = getValueFromObject(localVariableResponse.get("title"), String.class);
        String executionType = getValueFromObject(localVariableResponse.get("executionType"), String.class);
        boolean autoAssigned = false;
        String taskSystem = getValueFromObject(localVariableResponse.get("taskSystem"), String.class);
        String jurisdiction = getValueFromObject(localVariableResponse.get("jurisdiction"), String.class);
        String region = getValueFromObject(localVariableResponse.get("region"), String.class);
        String location = getValueFromObject(localVariableResponse.get("location"), String.class);
        String locationName = getValueFromObject(localVariableResponse.get("locationName"), String.class);
        String caseTypeId = getValueFromObject(localVariableResponse.get("caseTypeId"), String.class);
        String caseId = getValueFromObject(localVariableResponse.get("ccdId"), String.class);
        String caseName = getValueFromObject(localVariableResponse.get("caseName"), String.class);
        String caseCategory = getValueFromObject(localVariableResponse.get("appealType"), String.class);

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

    private <T> T getValueFromObject(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
