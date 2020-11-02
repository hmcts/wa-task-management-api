package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.Task;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;


@Service
public class TaskMapper {


    private final CamundaObjectMapper camundaObjectMapper;

    @Autowired
    public TaskMapper(CamundaObjectMapper camundaObjectMapper) {
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public Task mapToTaskObject(Map<String, CamundaVariable> variables, CamundaTask camundaTask) {
        // Camunda Attributes
        String id = camundaTask.getId();
        String name = camundaTask.getName();
        String task = camundaTask.getFormKey();
        ZonedDateTime createdDate = camundaTask.getCreated();
        ZonedDateTime dueDate = camundaTask.getDue();
        String assignee = camundaTask.getAssignee();
        // Local Variables
        String taskState = getVariableValue(variables.get("taskState"), String.class);
        String securityClassification = getVariableValue(variables
                                                               .get("securityClassification"), String.class);
        String taskTitle = getVariableValue(variables.get("title"), String.class);
        String executionType = getVariableValue(variables.get("executionType"), String.class);
        boolean autoAssigned = false;
        String taskSystem = getVariableValue(variables.get("taskSystem"), String.class);
        String jurisdiction = getVariableValue(variables.get("jurisdiction"), String.class);
        String region = getVariableValue(variables.get("region"), String.class);
        String location = getVariableValue(variables.get("location"), String.class);
        String locationName = getVariableValue(variables.get("locationName"), String.class);
        String caseTypeId = getVariableValue(variables.get("caseTypeId"), String.class);
        String caseId = getVariableValue(variables.get("ccdId"), String.class);
        String caseName = getVariableValue(variables.get("caseName"), String.class);
        String caseCategory = getVariableValue(variables.get("appealType"), String.class);

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

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
