package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_BUSINESS_CONTEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DESCRIPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_LOCATION_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_MAJOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_MINOR_PRIORITY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_REGION_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TERMINATION_REASON;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WORK_TYPE;


@Service
@SuppressWarnings({"PMD.LinguisticNaming", "PMD.ExcessiveImports"})
public class CFTTaskMapper {

    @Autowired
    private ObjectMapper objectMapper;

    public TaskResource mapToTaskObject(String taskId, List<TaskAttribute> taskAttributes) {

        Map<TaskAttributeDefinition, Object> attributes = taskAttributes.stream()
            .filter(attribute -> attribute.getValue() != null)
            .collect(Collectors.toMap(TaskAttribute::getName, TaskAttribute::getValue));

        List<NoteResource> notes = extractWarningNotes(attributes);
        ExecutionTypeResource executionTypeResource = extractExecutionType(attributes);
        OffsetDateTime dueDate = readDate(attributes, TASK_DUE_DATE, null);
        OffsetDateTime createdDate = readDate(attributes, TASK_CREATED, null);

        return new TaskResource(
            taskId,
            read(attributes, TASK_NAME, null),
            read(attributes, TASK_TYPE, null),
            dueDate,
            read(attributes, TASK_STATE, null),
            read(attributes, TASK_SYSTEM, null),
            read(attributes, TASK_SECURITY_CLASSIFICATION, null),
            read(attributes, TASK_TITLE, null),
            read(attributes, TASK_DESCRIPTION, null),
            notes,
            read(attributes, TASK_MAJOR_PRIORITY, null),
            read(attributes, TASK_MINOR_PRIORITY, null),
            read(attributes, TASK_ASSIGNEE, null),
            read(attributes, TASK_ASSIGNEE, false),
            executionTypeResource,
            read(attributes, TASK_WORK_TYPE, null),
            read(attributes, TASK_ROLE_CATEGORY, null),
            read(attributes, TASK_HAS_WARNINGS, false),
            read(attributes, TASK_ASSIGNMENT_EXPIRY, null),
            read(attributes, TASK_CASE_ID, null),
            read(attributes, TASK_CASE_TYPE_ID, null),
            read(attributes, TASK_CASE_NAME, null),
            read(attributes, TASK_JURISDICTION, null),
            read(attributes, TASK_REGION, null),
            read(attributes, TASK_REGION_NAME, null),
            read(attributes, TASK_LOCATION, null),
            read(attributes, TASK_LOCATION_NAME, null),
            read(attributes, TASK_BUSINESS_CONTEXT, null),
            read(attributes, TASK_TERMINATION_REASON, null),
            createdDate,
            read(attributes, TASK_ROLES, null)
        );
    }

    private ExecutionTypeResource extractExecutionType(Map<TaskAttributeDefinition, Object> attributes) {
        String executionTypeName = read(attributes, TASK_EXECUTION_TYPE_NAME, null);

        if (executionTypeName != null) {
            Optional<ExecutionType> value = ExecutionType.from(executionTypeName);
            if (value.isPresent()) {
                return new ExecutionTypeResource(
                    value.get(),
                    value.get().getName(),
                    value.get().getDescription()
                );
            } else {
                throw new IllegalStateException(
                    "executionTypeName " + executionTypeName + " could not be mapped to ExecutionType enum"
                );
            }
        }
        return null;
    }

    private List<NoteResource> extractWarningNotes(Map<TaskAttributeDefinition, Object> attributes) {
        List<NoteResource> notes = null;
        WarningValues warningList = read(attributes, TASK_WARNINGS, null);
        if (warningList != null) {
            List<Warning> warnings = warningList.getValues();
            if (!warnings.isEmpty()) {
                notes = warnings.stream()
                    .map(warning -> new NoteResource(
                        warning.getWarningCode(),
                        "WARNING",
                        null,
                        OffsetDateTime.now(),
                        warning.getWarningText()
                    )).collect(Collectors.toList());
            }
        }
        return notes;
    }

    @SuppressWarnings("unchecked")
    private <T> T read(Map<TaskAttributeDefinition, Object> attributesMap,
                       TaskAttributeDefinition extractor,
                       Object defaultValue) {
        return (T) map(attributesMap, extractor).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T readDate(Map<TaskAttributeDefinition, Object> attributesMap,
                           TaskAttributeDefinition extractor,
                           Object defaultValue) {
        Optional<T> maybeValue = map(attributesMap, extractor);
        if (maybeValue.isPresent()) {
            return (T) OffsetDateTime.parse((String) maybeValue.get(), CamundaTime.CAMUNDA_DATA_TIME_FORMATTER);
        } else {
            return (T) defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> map(Map<TaskAttributeDefinition, Object> object, TaskAttributeDefinition extractor) {

        if (object == null) {
            return Optional.empty();
        }
        Object obj = object.get(extractor);
        Object value = objectMapper.convertValue(obj, extractor.getTypeReference());

        return value == null ? Optional.empty() : Optional.of((T) value);
    }
}

