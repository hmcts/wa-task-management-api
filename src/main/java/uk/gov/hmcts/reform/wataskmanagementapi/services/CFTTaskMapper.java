package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ADDITIONAL_PROPERTIES;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NEXT_HEARING_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NEXT_HEARING_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_PRIORITY_DATE;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;


@Service
@SuppressWarnings(
    {"PMD.LinguisticNaming", "PMD.ExcessiveImports", "PMD.DataflowAnomalyAnalysis",
        "PMD.NcssCount", "PMD.CyclomaticComplexity", "PMD.TooManyMethods", "PMD.GodClass", "java:S5411",
        "PMD.ExcessiveMethodLength", "PMD.NPathComplexity"})
@Slf4j
public class CFTTaskMapper {

    private final ObjectMapper objectMapper;

    public CFTTaskMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
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
        OffsetDateTime createdDate = readDate(attributes, TASK_CREATED, ZonedDateTime.now().toOffsetDateTime());
        OffsetDateTime priorityDate = readDate(attributes, TASK_PRIORITY_DATE, null);

        Objects.requireNonNull(dueDate, "TASK_DUE_DATE must not be null");
        if (priorityDate == null) {
            priorityDate = dueDate;
        }

        WorkTypeResource workTypeResource = extractWorkType(attributes);
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
            read(attributes, TASK_MAJOR_PRIORITY, 5000),
            read(attributes, TASK_MINOR_PRIORITY, 500),
            read(attributes, TASK_ASSIGNEE, null),
            read(attributes, TASK_AUTO_ASSIGNED, false),
            executionTypeResource,
            workTypeResource,
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
            read(attributes, TASK_CASE_CATEGORY, null),
            read(attributes, TASK_ADDITIONAL_PROPERTIES, null),
            read(attributes, TASK_NEXT_HEARING_ID, null),
            readDate(attributes, TASK_NEXT_HEARING_DATE, null),
            priorityDate
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

    public TaskResource reconfigureTaskResourceFromDmnResults(TaskResource taskResource,
                                                              TaskConfigurationResults taskConfigurationResults) {

        List<ConfigurationDmnEvaluationResponse> configurationDmnResponse = taskConfigurationResults
            .getConfigurationDmnResponse();
        configurationDmnResponse.forEach(response -> reconfigureTaskAttribute(
            taskResource,
            response.getName().getValue(),
            response.getValue().getValue(),
            response.getCanReconfigure() != null && response.getCanReconfigure().getValue()
            )
        );

        List<PermissionsDmnEvaluationResponse> permissions = taskConfigurationResults.getPermissionsDmnResponse();
        taskResource.setTaskRoleResources(mapPermissions(permissions, taskResource));
        return taskResource;
    }

    @SuppressWarnings("PMD.NPathComplexity")
    public Task mapToTaskWithPermissions(TaskResource taskResource, Set<PermissionTypes> permissionsUnionForUser) {
        return new Task(
            taskResource.getTaskId(),
            taskResource.getTaskName(),
            taskResource.getTaskType(),
            taskResource.getState().getValue().toLowerCase(Locale.ROOT),
            taskResource.getTaskSystem() == null ? null : taskResource.getTaskSystem().getValue(),
            taskResource.getSecurityClassification() == null ? null : taskResource.getSecurityClassification()
                .getSecurityClassification(),
            taskResource.getTitle(),
            taskResource.getCreated().toZonedDateTime(),
            taskResource.getDueDateTime().toZonedDateTime(),
            taskResource.getAssignee(),
            taskResource.getAutoAssigned(),
            taskResource.getExecutionTypeCode() == null ? null : taskResource.getExecutionTypeCode().getExecutionName(),
            taskResource.getJurisdiction(),
            taskResource.getRegion(),
            taskResource.getLocation(),
            taskResource.getLocationName(),
            taskResource.getCaseTypeId(),
            taskResource.getCaseId(),
            taskResource.getCaseCategory(),
            taskResource.getCaseName(),
            taskResource.getHasWarnings(),
            mapNoteResourceToWarnings(taskResource.getNotes()),
            taskResource.getCaseCategory(),
            taskResource.getWorkTypeResource() == null ? null : taskResource.getWorkTypeResource().getId(),
            taskResource.getWorkTypeResource() == null ? null : taskResource.getWorkTypeResource().getLabel(),
            new TaskPermissions(permissionsUnionForUser),
            taskResource.getRoleCategory(),
            taskResource.getDescription(),
            taskResource.getAdditionalProperties(),
            taskResource.getNextHearingId(),
            taskResource.getNextHearingDate() == null ? null : taskResource.getNextHearingDate().toZonedDateTime(),
            taskResource.getMinorPriority(),
            taskResource.getMajorPriority(),
            taskResource.getPriorityDate() == null ? null : taskResource.getPriorityDate().toZonedDateTime(),
            taskResource.getReconfigureRequestTime() == null ? null
                : taskResource.getReconfigureRequestTime().toZonedDateTime(),
            taskResource.getLastReconfigurationTime() == null ? null
                : taskResource.getLastReconfigurationTime().toZonedDateTime()
        );
    }


    public Task mapToTaskAndExtractPermissionsUnion(TaskResource taskResource, List<RoleAssignment> roleAssignments) {
        Set<PermissionTypes> permissionsUnionForUser =
            extractUnionOfPermissionsForUser(
                taskResource.getTaskRoleResources(),
                roleAssignments
            );

        return mapToTaskWithPermissions(taskResource, permissionsUnionForUser);
    }

    @SuppressWarnings("unchecked")
    public <T> T read(Map<TaskAttributeDefinition, Object> attributesMap,
                      TaskAttributeDefinition extractor,
                      Object defaultValue) {
        return (T) map(attributesMap, extractor).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T readDate(Map<TaskAttributeDefinition, Object> attributesMap,
                          TaskAttributeDefinition extractor,
                          Object defaultValue) {
        Optional<T> maybeValue = map(attributesMap, extractor);
        if (maybeValue.isPresent()) {
            return (T) OffsetDateTime.parse((String) maybeValue.get(), CAMUNDA_DATA_TIME_FORMATTER);
        } else {
            return (T) defaultValue;
        }
    }

    public Map<String, Object> getTaskAttributes(TaskResource taskResource) {
        return objectMapper.convertValue(taskResource, new TypeReference<HashMap<String, Object>>() {
        });
    }

    public Set<PermissionTypes> extractUnionOfPermissionsForUser(Set<TaskRoleResource> taskRoleResources,
                                                                 List<RoleAssignment> roleAssignments) {
        List<String> userRoleNames = roleAssignments.stream()
            .map(RoleAssignment::getRoleName)
            .collect(Collectors.toList());

        TreeSet<PermissionTypes> permissionsFound = new TreeSet<>();

        if (taskRoleResources != null) {
            taskRoleResources.forEach(taskRoleResource -> {
                if (userRoleNames.contains(taskRoleResource.getRoleName())) {
                    Set<PermissionTypes> permissionTypes = evaluatePermissionsFoundAndCollectResults(taskRoleResource);
                    permissionsFound.addAll(permissionTypes);
                }
            });
        }

        return permissionsFound;
    }

    public TaskRolePermissions mapToTaskRolePermissions(TaskRoleResource taskRoleResource) {
        List<String> authorisations = asList(taskRoleResource.getAuthorizations());

        final Set<PermissionTypes> permissionTypes = extractUnionOfPermissions(Set.of(taskRoleResource));

        return new TaskRolePermissions(
            taskRoleResource.getRoleCategory(),
            taskRoleResource.getRoleName(),
            List.copyOf(permissionTypes),
            authorisations
        );
    }

    private Set<PermissionTypes> evaluatePermissionsFoundAndCollectResults(TaskRoleResource taskRoleResource) {
        Set<PermissionTypes> accumulator = new HashSet<>();
        if (taskRoleResource.getRead()) {
            accumulator.add(PermissionTypes.READ);
        }
        if (taskRoleResource.getManage()) {
            accumulator.add(PermissionTypes.MANAGE);
        }
        if (taskRoleResource.getExecute()) {
            accumulator.add(PermissionTypes.EXECUTE);
        }
        if (taskRoleResource.getCancel()) {
            accumulator.add(PermissionTypes.CANCEL);
        }
        if (taskRoleResource.getOwn()) {
            accumulator.add(PermissionTypes.OWN);
        }
        if (taskRoleResource.getRefer()) {
            accumulator.add(PermissionTypes.REFER);
        }
        return accumulator;
    }

    private WorkTypeResource extractWorkType(Map<TaskAttributeDefinition, Object> attributes) {
        String workTypeId = read(attributes, TASK_WORK_TYPE, null);
        return workTypeId == null ? null : new WorkTypeResource(workTypeId);
    }

    private Set<PermissionTypes> extractUnionOfPermissions(Set<TaskRoleResource> taskRoleResources) {
        //Using TreeSet to benefit from SortedSet
        TreeSet<PermissionTypes> permissionsFound = new TreeSet<>();
        if (taskRoleResources != null) {
            taskRoleResources.forEach(
                taskRoleResource -> {
                    Set<PermissionTypes> permissionTypes = evaluatePermissionsFoundAndCollectResults(taskRoleResource);
                    permissionsFound.addAll(permissionTypes);
                }
            );
        }
        return permissionsFound;
    }

    private Set<TaskRoleResource> mapPermissions(
        List<PermissionsDmnEvaluationResponse> permissions,
        TaskResource taskResource
    ) {

        return permissions.stream()
            .map(permission -> {
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
                if (permission.getAssignmentPriority() != null
                    && permission.getAssignmentPriority().getValue() != null) {
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
                case WORK_TYPE:
                    WorkTypeResource workTypeResource = new WorkTypeResource((String) value, StringUtils.EMPTY);
                    taskResource.setWorkTypeResource(workTypeResource);
                    break;
                case CASE_ID:
                    taskResource.setCaseId((String) value);
                    break;
                case TASK_ID:
                    taskResource.setTaskId((String) value);
                    break;
                case TASK_NAME:
                    taskResource.setTaskName((String) value);
                    break;
                case ROLE_CATEGORY:
                    taskResource.setRoleCategory((String) value);
                    break;
                case DESCRIPTION:
                    taskResource.setDescription((String) value);
                    break;
                case ADDITIONAL_PROPERTIES:
                    Map<String, String> additionalProperties = extractAdditionalProperties(value);
                    taskResource.setAdditionalProperties(additionalProperties);
                    break;
                case NEXT_HEARING_ID:
                    if (value != null && Strings.isNotBlank((String) value)) {
                        taskResource.setNextHearingId((String) value);
                    }
                    break;
                case NEXT_HEARING_DATE:
                    if (value != null && Strings.isNotBlank((String) value)) {
                        taskResource.setNextHearingDate(ZonedDateTime.parse((String) value).toOffsetDateTime());
                    }
                    break;
                case MINOR_PRIORITY:
                    setMinorPriority(value, taskResource);
                    break;
                case MAJOR_PRIORITY:
                    setMajorPriority(value, taskResource);
                    break;
                case PRIORITY_DATE:
                    if (value instanceof String) {
                        if (Strings.isNotBlank((String) value)) {
                            taskResource.setPriorityDate(OffsetDateTime.parse((String) value));
                        }
                    } else {
                        taskResource.setPriorityDate((OffsetDateTime) value);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    void setMajorPriority(Object value, TaskResource taskResource) {
        if (value instanceof String) {
            taskResource.setMajorPriority(Integer.parseInt((String) value));
        } else {
            taskResource.setMajorPriority((Integer) value);
        }
    }

    void setMinorPriority(Object value, TaskResource taskResource) {
        if (value instanceof String) {
            taskResource.setMinorPriority(Integer.parseInt((String) value));
        } else {
            taskResource.setMinorPriority((Integer) value);
        }
    }

    protected void reconfigureTaskAttribute(TaskResource taskResource,
                                          String key,
                                          Object value,
                                          boolean canReconfigure) {
        Optional<CamundaVariableDefinition> enumKey = CamundaVariableDefinition.from(key);
        if (enumKey.isPresent() & canReconfigure) {
            switch (enumKey.get()) {
                case CASE_NAME:
                    taskResource.setCaseName((String) value);
                    break;
                case REGION:
                    taskResource.setRegion((String) value);
                    break;
                case LOCATION:
                    taskResource.setLocation((String) value);
                    break;
                case LOCATION_NAME:
                    taskResource.setLocationName((String) value);
                    break;
                case CASE_MANAGEMENT_CATEGORY:
                    taskResource.setCaseCategory((String) value);
                    break;
                case WORK_TYPE:
                    WorkTypeResource workTypeResource = new WorkTypeResource((String) value, StringUtils.EMPTY);
                    taskResource.setWorkTypeResource(workTypeResource);
                    break;
                case ROLE_CATEGORY:
                    taskResource.setRoleCategory((String) value);
                    break;
                case DESCRIPTION:
                    taskResource.setDescription((String) value);
                    break;
                case ADDITIONAL_PROPERTIES:
                    Map<String, String> additionalProperties = extractAdditionalProperties(value);
                    taskResource.setAdditionalProperties(additionalProperties);
                    break;
                case PRIORITY_DATE:
                    if (value instanceof String) {
                        if (Strings.isNotBlank((String) value)) {
                            taskResource.setPriorityDate(OffsetDateTime.parse((String) value));
                        }
                    } else {
                        taskResource.setPriorityDate((OffsetDateTime) value);
                    }
                    break;
                case MINOR_PRIORITY:
                    setMinorPriority(value, taskResource);
                    break;
                case MAJOR_PRIORITY:
                    setMajorPriority(value, taskResource);
                    break;
                case NEXT_HEARING_ID:
                    if (value != null && Strings.isNotBlank((String) value)) {
                        taskResource.setNextHearingId((String) value);
                    }
                    break;
                case NEXT_HEARING_DATE:
                    if (value instanceof String) {
                        if (Strings.isNotBlank((String) value)) {
                            taskResource.setNextHearingDate(OffsetDateTime.parse((String) value));
                        }
                    } else {
                        taskResource.setNextHearingDate((OffsetDateTime) value);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private Map<String, String> extractAdditionalProperties(Object value) {
        if (value != null) {
            try {
                return objectMapper.readValue((String) value, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Additional Properties mapping issue.", e);
            }
        }
        return null;
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
    private <T> Optional<T> map(Map<TaskAttributeDefinition, Object> object, TaskAttributeDefinition extractor) {

        if (object == null) {
            return Optional.empty();
        }
        Object obj = object.get(extractor);
        Object value = objectMapper.convertValue(obj, extractor.getTypeReference());

        return value == null ? Optional.empty() : Optional.of((T) value);
    }
}

