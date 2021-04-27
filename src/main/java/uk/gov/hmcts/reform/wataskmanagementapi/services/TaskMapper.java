package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.APPEAL_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;


@Service
@SuppressWarnings("PMD.LinguisticNaming")
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
        ZonedDateTime createdDate = camundaTask.getCreated();
        ZonedDateTime dueDate = camundaTask.getDue();
        String assignee = camundaTask.getAssignee();
        // Local Variables
        String type = getVariableValue(variables.get(TASK_TYPE.value()), String.class);
        String taskState = getVariableValue(variables.get(TASK_STATE.value()), String.class);
        String securityClassification = getVariableValue(variables.get(SECURITY_CLASSIFICATION.value()), String.class);
        String taskTitle = getVariableValue(variables.get(TITLE.value()), String.class);
        String executionType = getVariableValue(variables.get(EXECUTION_TYPE.value()), String.class);
        boolean autoAssigned = false;
        String taskSystem = getVariableValue(variables.get(TASK_SYSTEM.value()), String.class);
        String jurisdiction = getVariableValue(variables.get(JURISDICTION.value()), String.class);
        String region = getVariableValue(variables.get(REGION.value()), String.class);
        String location = getVariableValue(variables.get(LOCATION.value()), String.class);
        String locationName = getVariableValue(variables.get(LOCATION_NAME.value()), String.class);
        String caseTypeId = getVariableValue(variables.get(CASE_TYPE_ID.value()), String.class);
        String caseId = getVariableValue(variables.get(CASE_ID.value()), String.class);
        String caseName = getVariableValue(variables.get(CASE_NAME.value()), String.class);
        String caseCategory = getVariableValue(variables.get(APPEAL_TYPE.value()), String.class);
        Boolean hasWarnings = getVariableValue(variables.get(HAS_WARNINGS.value()), Boolean.class);

        return new Task(
            id,
            name,
            type,
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
            caseName,
            hasWarnings

        );
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }
}
