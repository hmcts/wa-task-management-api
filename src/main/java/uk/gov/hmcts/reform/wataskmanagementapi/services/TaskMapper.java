package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ADDITIONAL_PROPERTIES;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.APPEAL_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_MANAGEMENT_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DESCRIPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.LOCATION_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.NEXT_HEARING_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.NEXT_HEARING_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WARNING_LIST;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WORK_TYPE;


@Service
@SuppressWarnings({"PMD.LinguisticNaming", "PMD.ExcessiveImports"})
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
        WarningValues warningList = getVariableValue(variables.get(WARNING_LIST.value()), WarningValues.class);
        String caseManagementCategory = getVariableValue(variables.get(CASE_MANAGEMENT_CATEGORY.value()), String.class);
        String workType = getVariableValue(variables.get(WORK_TYPE.value()), String.class);
        String description = getVariableValue(variables.get(DESCRIPTION.value()), String.class);
        ConcurrentHashMap<String, String> additionalProperties
            = getTypedVariableValue(variables.get(ADDITIONAL_PROPERTIES.value()), new TypeReference<>() {});
        String nextHearingId = getVariableValue(variables.get(NEXT_HEARING_ID.value()), String.class);
        ZonedDateTime nextHearingDate = getVariableValue(variables.get(NEXT_HEARING_DATE.value()), ZonedDateTime.class);
        Integer minorPriority = getVariableValue(variables.get(MINOR_PRIORITY.value()), Integer.class);
        Integer majorPriority = getVariableValue(variables.get(MAJOR_PRIORITY.value()), Integer.class);
        ZonedDateTime priorityDate = getVariableValue(variables.get(PRIORITY_DATE.value()), ZonedDateTime.class);
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
            hasWarnings,
            warningList,
            caseManagementCategory,
            workType,
            // returning null as it's for R2
            null,
            //returning empty since this should only be used in R1 and task permissions is R2
            new TaskPermissions(Collections.emptySet()),
            // returning null as its only applicable for R2
            null,
            description,
            additionalProperties,
            nextHearingId,
            nextHearingDate,
            minorPriority,
            majorPriority,
            priorityDate
        );
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type) {
        Optional<T> value = camundaObjectMapper.read(variable, type);
        return value.orElse(null);
    }

    private <T> T getTypedVariableValue(CamundaVariable variable, TypeReference<T> typeReference) {
        return camundaObjectMapper.read(variable, typeReference).orElse(null);
    }
}
