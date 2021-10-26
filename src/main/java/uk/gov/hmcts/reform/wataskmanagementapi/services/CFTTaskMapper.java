package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_BUSINESS_CONTEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_CATEGORY;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TERMINATION_REASON;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WORK_TYPE;


@Service
@SuppressWarnings(
    {"PMD.LinguisticNaming", "PMD.ExcessiveImports", "PMD.DataflowAnomalyAnalysis",
        "PMD.NcssCount", "PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
@Slf4j
public class CFTTaskMapper {

    private final ObjectMapper objectMapper;

    @Autowired
    public CFTTaskMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskResource mapToTaskResource(String taskId, List<TaskAttribute> taskAttributes) {
        log.debug("mapping task attributes to taskResource: taskAttributes({})", taskAttributes);
        Map<TaskAttributeDefinition, Object> attributes = taskAttributes.stream()
            .filter(attribute -> {
                log.debug("filtering out null attributes: attribute({})", attribute);
                return attribute != null && attribute.getValue() != null;
            })
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
            CFTTaskState.UNCONFIGURED,
            read(attributes, TASK_SYSTEM, null),
            read(attributes, TASK_SECURITY_CLASSIFICATION, null),
            read(attributes, TASK_TITLE, null),
            read(attributes, TASK_DESCRIPTION, null),
            notes,
            read(attributes, TASK_MAJOR_PRIORITY, null),
            read(attributes, TASK_MINOR_PRIORITY, null),
            read(attributes, TASK_ASSIGNEE, null),
            read(attributes, TASK_AUTO_ASSIGNED, false),
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
            read(attributes, TASK_ROLES, null),
            read(attributes, TASK_CASE_CATEGORY, null)
        );
    }

    public TaskResource mapConfigurationAttributes(TaskResource taskResource,
                                                   TaskConfigurationResults taskConfigurationResults) {

        //Update Task Resource with configuration variables
        taskConfigurationResults.getProcessVariables()
            .forEach((key, value) -> mapVariableToTaskResourceProperty(taskResource, key, value));

        List<PermissionsDmnEvaluationResponse> permissions = taskConfigurationResults.getPermissionsDmnResponse();
        taskResource.setTaskRoleResources(mapPermissions(permissions, taskResource));
        return taskResource;
    }

    private Set<TaskRoleResource> mapPermissions(
        List<PermissionsDmnEvaluationResponse> permissions, TaskResource taskResource
    ) {

        return permissions.stream().map(permission -> {

            Objects.requireNonNull(permission.getName(), "Permissions name cannot be null");
            Objects.requireNonNull(permission.getValue(), "Permissions value cannot be null");
            final String roleName = permission.getName().getValue();
            final String permissionsValue = permission.getValue().getValue();

            final Set<PermissionTypes> permissionsFound = Arrays.stream(permissionsValue.split(","))
                .map(p -> PermissionTypes.from(p).orElse(null))
                .collect(Collectors.toSet());
            List<String> authorisations = new ArrayList<>();
            if (permission.getAuthorisations() != null && permission.getAuthorisations().getValue() != null) {
                authorisations.addAll(asList(permission.getAuthorisations().getValue().split(",")));
            }

            Integer assignmentPriority = null;
            if (permission.getAssignmentPriority() != null && permission.getAssignmentPriority().getValue() != null) {
                assignmentPriority = permission.getAssignmentPriority().getValue();
            }
            boolean autoAssignable = false;
            if (permission.getAutoAssignable() != null && permission.getAutoAssignable().getValue() != null) {
                autoAssignable = Boolean.TRUE.equals(permission.getAutoAssignable().getValue());
            }

            String roleCategory = null;
            if (permission.getRoleCategory() != null && permission.getRoleCategory().getValue() != null) {
                roleCategory = permission.getRoleCategory().getValue();
            }

            return new TaskRoleResource(
                roleName,
                permissionsFound.contains(PermissionTypes.READ),
                permissionsFound.contains(PermissionTypes.OWN),
                permissionsFound.contains(PermissionTypes.EXECUTE),
                permissionsFound.contains(PermissionTypes.MANAGE),
                permissionsFound.contains(PermissionTypes.CANCEL),
                permissionsFound.contains(PermissionTypes.REFER),
                authorisations.toArray(new String[0]),
                assignmentPriority,
                autoAssignable,
                roleCategory,
                taskResource.getTaskId(),
                ZonedDateTime.now().toOffsetDateTime()
            );
        }).collect(Collectors.toSet());
    }

    private void mapVariableToTaskResourceProperty(TaskResource taskResource, String key, Object value) {

        Optional<CamundaVariableDefinition> enumKey = CamundaVariableDefinition.from(key);
        if (enumKey.isPresent()) {

            switch (enumKey.get()) {
                case AUTO_ASSIGNED:
                    taskResource.setAutoAssigned((Boolean) value);
                    break;
                case ASSIGNEE:
                    taskResource.setAssignee((String) value);
                    break;
                case CASE_ID:
                    taskResource.setCaseId((String) value);
                    break;
                case CASE_NAME:
                    taskResource.setCaseName((String) value);
                    break;
                case CASE_TYPE_ID:
                    taskResource.setCaseTypeId((String) value);
                    break;
                case EXECUTION_TYPE:
                    Optional<ExecutionType> executionType = ExecutionType.from((String) value);
                    if (executionType.isPresent()) {
                        taskResource.setExecutionTypeCode(new ExecutionTypeResource(
                            executionType.get(),
                            executionType.get().getName(),
                            executionType.get().getDescription()
                        ));
                    } else {
                        throw new IllegalStateException("Could not map executionType to ExecutionType enum");
                    }
                    break;
                case JURISDICTION:
                    taskResource.setJurisdiction((String) value);
                    break;
                case LOCATION:
                    taskResource.setLocation((String) value);
                    break;
                case LOCATION_NAME:
                    taskResource.setLocationName((String) value);
                    break;
                case REGION:
                    taskResource.setRegion((String) value);
                    break;
                case SECURITY_CLASSIFICATION:
                    SecurityClassification sc = SecurityClassification.valueOf((String) value);
                    taskResource.setSecurityClassification(sc);
                    break;
                case TASK_ID:
                    taskResource.setTaskId((String) value);
                    break;
                case TASK_NAME:
                    taskResource.setTaskName((String) value);
                    break;
                case TASK_SYSTEM:
                    TaskSystem taskSystem = TaskSystem.valueOf((String) value);
                    taskResource.setTaskSystem(taskSystem);
                    break;
                case TASK_TYPE:
                    taskResource.setTaskType((String) value);
                    break;
                case TITLE:
                    taskResource.setTitle((String) value);
                    break;
                case HAS_WARNINGS:
                    taskResource.setHasWarnings((Boolean) value);
                    break;
                case CASE_MANAGEMENT_CATEGORY:
                    taskResource.setCaseCategory((String) value);
                    break;
                default:
                    break;
            }
        }
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
                    "ExecutionTypeName value: '" + executionTypeName + "' could not be mapped to ExecutionType enum"
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
                        warning.getWarningText()
                    )).collect(Collectors.toList());
            }
        }
        return notes;
    }

    private WarningValues mapNoteResourceToWarnings(List<NoteResource> notes) {

        if (notes != null) {
            List<Warning> warnings = notes.stream()
                .filter(noteResource -> "WARNING".equals(noteResource.getNoteType()))
                .map(noteResource -> new Warning(noteResource.getCode(), noteResource.getContent()))
                .collect(Collectors.toList());
            return new WarningValues(warnings);
        }
        return new WarningValues();
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


    public Task mapToTask(TaskResource taskResource) {
        return new Task(taskResource.getTaskId(),
            taskResource.getTaskName(),
            taskResource.getTaskType(),
            taskResource.getState().getValue().toLowerCase(Locale.ROOT),
            taskResource.getTaskSystem().getValue(),
            taskResource.getSecurityClassification().getSecurityClassification(),
            taskResource.getTitle(),
            taskResource.getCreated() == null ? null : taskResource.getCreated().toZonedDateTime(),
            taskResource.getDueDateTime() == null ? null : taskResource.getDueDateTime().toZonedDateTime(),
            taskResource.getAssignee(),
            taskResource.getAutoAssigned(),
            taskResource.getExecutionTypeCode().getExecutionName(),
            taskResource.getJurisdiction(),
            taskResource.getRegion(),
            taskResource.getLocation(),
            taskResource.getLocationName(),
            taskResource.getCaseTypeId(),
            taskResource.getCaseId(),
            taskResource.getRoleCategory(),
            taskResource.getCaseName(),
            taskResource.getHasWarnings(),
            mapNoteResourceToWarnings(taskResource.getNotes()),
            taskResource.getCaseCategory());
    }
}

