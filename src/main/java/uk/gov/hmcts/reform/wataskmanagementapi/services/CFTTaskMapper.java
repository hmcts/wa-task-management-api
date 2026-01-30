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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ReconfigureInputVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.CreateTaskRequestTask;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WARNING_LIST;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService.ADDITIONAL_PROPERTIES_PREFIX;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DueDateCalculator.DATE_TIME_FORMATTER;


@Service
@SuppressWarnings(
    {"PMD.LinguisticNaming", "PMD.ExcessiveImports", "PMD.DataflowAnomalyAnalysis",
        "PMD.NcssCount", "PMD.CyclomaticComplexity", "PMD.TooManyMethods", "PMD.GodClass", "java:S5411",
        "PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.AvoidDuplicateLiterals",
        "PMD.CognitiveComplexity", "PMD.ReturnEmptyCollectionRatherThanNull", "PMD.NullAssignment","PMD.LawOfDemeter"
    })
@Slf4j
public class CFTTaskMapper {

    private final ObjectMapper objectMapper;

    public CFTTaskMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public TaskResource mapToTaskResource(String taskId, Map<String, Object> taskAttributes) {
        log.debug("mapping task attributes to taskResource: taskAttributes({})", taskAttributes);
        Map<CamundaVariableDefinition, Object> attributes = taskAttributes.entrySet().stream()
            .filter(key -> CamundaVariableDefinition.from(key.getKey()).isPresent())
            .collect(Collectors.toMap(
                key -> CamundaVariableDefinition.from(key.getKey()).get(),
                Map.Entry::getValue
            ));

        List<NoteResource> notes = extractWarningNotes(attributes);
        ExecutionTypeResource executionTypeResource = extractExecutionType(attributes);
        OffsetDateTime dueDate = readDate(attributes, DUE_DATE, null);
        OffsetDateTime createdDate = readDate(attributes, CREATED, ZonedDateTime.now().toOffsetDateTime());
        OffsetDateTime priorityDate = readDate(attributes, PRIORITY_DATE, null);

        Objects.requireNonNull(dueDate, "DUE_DATE must not be null");
        if (priorityDate == null) {
            priorityDate = dueDate;
        }

        WorkTypeResource workTypeResource = extractWorkType(attributes);
        return new TaskResource(
            taskId,
            read(attributes, CamundaVariableDefinition.TASK_NAME, null),
            read(attributes, CamundaVariableDefinition.TASK_TYPE, null),
            dueDate,
            CFTTaskState.UNCONFIGURED,
            read(attributes, CamundaVariableDefinition.TASK_SYSTEM, null),
            read(attributes, CamundaVariableDefinition.SECURITY_CLASSIFICATION, null),
            read(attributes, CamundaVariableDefinition.TITLE, null),
            read(attributes, CamundaVariableDefinition.DESCRIPTION, null),
            notes,
            read(attributes, CamundaVariableDefinition.MAJOR_PRIORITY, 5000),
            read(attributes, CamundaVariableDefinition.MINOR_PRIORITY, 500),
            read(attributes, CamundaVariableDefinition.ASSIGNEE, null),
            read(attributes, CamundaVariableDefinition.AUTO_ASSIGNED, false),
            executionTypeResource,
            workTypeResource,
            read(attributes, CamundaVariableDefinition.ROLE_CATEGORY, null),
            read(attributes, CamundaVariableDefinition.HAS_WARNINGS, false),
            readDate(attributes, CamundaVariableDefinition.ASSIGNMENT_EXPIRY, null),
            read(attributes, CamundaVariableDefinition.CASE_ID, null),
            read(attributes, CamundaVariableDefinition.CASE_TYPE_ID, null),
            read(attributes, CamundaVariableDefinition.CASE_NAME, null),
            read(attributes, CamundaVariableDefinition.JURISDICTION, null),
            read(attributes, CamundaVariableDefinition.REGION, null),
            read(attributes, CamundaVariableDefinition.REGION_NAME, null),
            read(attributes, CamundaVariableDefinition.LOCATION, null),
            read(attributes, CamundaVariableDefinition.LOCATION_NAME, null),
            read(attributes, CamundaVariableDefinition.BUSINESS_CONTEXT, null),
            read(attributes, CamundaVariableDefinition.TERMINATION_REASON, null),
            createdDate,
            read(attributes, CamundaVariableDefinition.TASK_ROLES, null),
            read(attributes, CamundaVariableDefinition.CASE_CATEGORY, null),
            read(attributes, CamundaVariableDefinition.ADDITIONAL_PROPERTIES, null),
            read(attributes, CamundaVariableDefinition.NEXT_HEARING_ID, null),
            readDate(attributes, CamundaVariableDefinition.NEXT_HEARING_DATE, null),
            priorityDate
        );
    }

    public TaskResource mapToApiFirstTaskResource(CreateTaskRequestTask request) {
        log.info("mapping task attributes to taskResource");

        ExecutionType executionType = ExecutionType.fromJson(request.getExecutionType().getValue());
        ExecutionTypeResource executionTypeResource = new ExecutionTypeResource(
            executionType,
            executionType.getName(),
            executionType.getDescription()

        );
        String taskId = UUID.randomUUID().toString();

        Set<TaskRoleResource> taskRoleResources = mapPermissions(request, taskId);

        WorkTypeResource workTypeResource = new WorkTypeResource(
            request.getWorkType()
        );
        Map<String, String> additionalProperties = Collections.emptyMap();
        if (request.getAdditionalProperties() != null) {
            additionalProperties = request.getAdditionalProperties().entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> String.valueOf(e.getValue())
                ));
        }
        String taskTitle = request.getTitle();
        String taskName = request.getName();
        Integer majorPriority = request.getMajorPriority();
        Integer minorPriority = request.getMinorPriority();
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            request.getType(),
            request.getDueDateTime(),
            CFTTaskState.UNCONFIGURED,
            TaskSystem.valueOf(request.getTaskSystem().getValue()),
            SecurityClassification.valueOf(request.getSecurityClassification().getValue()),
            taskTitle != null ? taskTitle : taskName,
            request.getDescription(),
            new ArrayList<NoteResource>(),
            majorPriority != null ? majorPriority : 5000,
            minorPriority != null ? minorPriority : 500,
            null, //Need to get from taskPayload assignee
            false, //autoAssigned
            executionTypeResource,
            workTypeResource,
            request.getRoleCategory(),
            false, //has_warnings
            null, //assignment_expiry
            request.getCaseId(),
            request.getCaseTypeId(),
            request.getCaseName(),
            request.getJurisdiction(),
            request.getRegion(),
            request.getRegionName(),
            request.getLocation(),
            request.getLocationName(),
            null, //business_context
            null, //termination_reason
            request.getCreated(), // We set created time from request, can change it to now() if needed
            taskRoleResources, //task_roles
            request.getCaseCategory(),
            additionalProperties,
            null, //next_hearing_id
            null, //next_hearing_date

            request.getPriorityDate()
        );
        taskResource.setExternalTaskId(
            Optional.ofNullable(request.getExternalTaskId())
                .map(Object::toString)
                .orElse(null)
        );
        taskResource.setCamundaTask(false);
        return taskResource;
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
        configurationDmnResponse.stream().filter(response ->
                !response.getName().getValue().startsWith(ADDITIONAL_PROPERTIES_PREFIX))
                .forEach(response -> reconfigureTaskAttribute(
            taskResource,
            response.getName().getValue(),
            response.getValue().getValue(),
            response.getCanReconfigure() != null && response.getCanReconfigure().getValue()
            )
        );

        List<ConfigurationDmnEvaluationResponse> configurationAdditionalAttributeDmnResponse =
                configurationDmnResponse.stream().filter(response ->
                        response.getName().getValue().startsWith(ADDITIONAL_PROPERTIES_PREFIX)).toList();
        reconfigureAdditionalTaskAttribute(taskResource, configurationAdditionalAttributeDmnResponse);

        List<PermissionsDmnEvaluationResponse> permissions = taskConfigurationResults.getPermissionsDmnResponse();
        taskResource.setTaskRoleResources(mapPermissions(permissions, taskResource));
        return taskResource;
    }

    @SuppressWarnings("PMD.NPathComplexity")
    public Task mapToTaskWithPermissions(TaskResource taskResource, Set<PermissionTypes> permissionsUnionForUser) {
        Task task =  new Task(
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

        if (taskResource.getTerminationProcess() != null) {
            task.setTerminationProcess(taskResource.getTerminationProcess().getValue());
        }
        return task;

    }


    public Task mapToTaskAndExtractPermissionsUnion(TaskResource taskResource,
                                                    List<RoleAssignment> roleAssignments) {
        Set<PermissionTypes> permissionsUnionForUser =
            extractUnionOfPermissionsForUser(
                taskResource.getTaskRoleResources(),
                roleAssignments,
                taskResource.getCaseId()
            );

        return mapToTaskWithPermissions(taskResource, permissionsUnionForUser);
    }

    @SuppressWarnings("unchecked")
    public <T> T read(Map<CamundaVariableDefinition, Object> attributesMap,
                      CamundaVariableDefinition extractor,
                      Object defaultValue) {
        return (T) map(attributesMap, extractor).orElse(defaultValue);
    }


    @SuppressWarnings("unchecked")
    public <T> T readDate(Map<CamundaVariableDefinition, Object> attributesMap,
                          CamundaVariableDefinition extractor,
                          Object defaultValue) {
        Optional<T> maybeValue = map(attributesMap, extractor);
        if (maybeValue.isPresent()) {
            return (T) OffsetDateTime.parse((String) maybeValue.get(), CAMUNDA_DATA_TIME_FORMATTER);
        } else {
            return (T) defaultValue;
        }
    }

    public Map<String, Object> getTaskAttributes(TaskResource taskResource) {
        /*
        Below fields are not required for reconfiguration
        securityClassification
        notes
        autoAssigned
        assignmentExpiry
        businessContext
        executionTypeCode
        taskRoleResources
        reconfigureRequestTime
        lastReconfigurationTime
        lastUpdatedTimestamp
        lastUpdatedUser
        lastUpdatedAction
        indexed
        terminationReason
        taskSystem
        */
        ReconfigureInputVariableDefinition task =
            TaskEntityToReconfigureInputVariableDefMapper.INSTANCE.map(taskResource);
        return objectMapper.convertValue(task, new TypeReference<HashMap<String, Object>>() {});
    }

    public Set<PermissionTypes> extractUnionOfPermissionsForUser(Set<TaskRoleResource> taskRoleResources,
                                                                 List<RoleAssignment> roleAssignments) {
        Optional caseId = taskRoleResources.stream()
            .filter(t -> t.getTaskResource() != null
                && t.getTaskResource().getCaseId() != null).map(t -> t.getTaskResource().getCaseId())
            .findFirst();
        if (caseId.isPresent()) {
            return extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments,
                                                    (String) caseId.get());
        } else {
            return new TreeSet<PermissionTypes>();
        }
    }

    private Set<PermissionTypes> extractUnionOfPermissionsForUser(Set<TaskRoleResource> taskRoleResources,
                                                                 List<RoleAssignment> roleAssignments,
                                                                 String caseId) {
        TreeSet<PermissionTypes> permissionsFound = new TreeSet<>();
        if (caseId != null) {
            List<String> userRoleNames = roleAssignments.stream()
                .filter(ra -> !ra.getRoleType().equals(RoleType.CASE) || ra.getAttributes() != null
                    && ra.getAttributes().get("caseId") != null
                    && ra.getAttributes().get("caseId").equals(caseId))
                .map(RoleAssignment::getRoleName)
                .toList();


            if (taskRoleResources != null) {
                taskRoleResources.forEach(taskRoleResource -> {
                    if (userRoleNames.contains(taskRoleResource.getRoleName())) {
                        Set<PermissionTypes> permissionTypes = evaluatePermissionsFoundAndCollectResults(
                            taskRoleResource);
                        permissionsFound.addAll(permissionTypes);
                    }
                });
            }
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

        addPermission(accumulator, taskRoleResource.getRead(), PermissionTypes.READ);
        addPermission(accumulator, taskRoleResource.getManage(), PermissionTypes.MANAGE);
        addPermission(accumulator, taskRoleResource.getExecute(), PermissionTypes.EXECUTE);
        addPermission(accumulator, taskRoleResource.getCancel(), PermissionTypes.CANCEL);
        addPermission(accumulator, taskRoleResource.getOwn(), PermissionTypes.OWN);
        addPermission(accumulator, taskRoleResource.getRefer(), PermissionTypes.REFER);
        addPermission(accumulator, taskRoleResource.getClaim(), PermissionTypes.CLAIM);
        addPermission(accumulator, taskRoleResource.getAssign(), PermissionTypes.ASSIGN);
        addPermission(accumulator, taskRoleResource.getUnassign(), PermissionTypes.UNASSIGN);
        addPermission(accumulator, taskRoleResource.getUnassignAssign(), PermissionTypes.UNASSIGN_ASSIGN);
        addPermission(accumulator, taskRoleResource.getComplete(), PermissionTypes.COMPLETE);
        addPermission(accumulator, taskRoleResource.getCompleteOwn(), PermissionTypes.COMPLETE_OWN);
        addPermission(accumulator, taskRoleResource.getCancelOwn(), PermissionTypes.CANCEL_OWN);
        addPermission(accumulator, taskRoleResource.getUnassignClaim(), PermissionTypes.UNASSIGN_CLAIM);
        addPermission(accumulator, taskRoleResource.getUnclaim(), PermissionTypes.UNCLAIM);
        addPermission(accumulator, taskRoleResource.getUnclaimAssign(), PermissionTypes.UNCLAIM_ASSIGN);

        return accumulator;
    }

    private void addPermission(Set<PermissionTypes> accumulator, boolean condition, PermissionTypes permission) {
        if (condition) {
            accumulator.add(permission);
        }
    }

    private WorkTypeResource extractWorkType(Map<CamundaVariableDefinition, Object> attributes) {
        String workTypeId = read(attributes, WORK_TYPE, null);
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

                    .map(String:: trim)
                    .map(p -> PermissionTypes.from(p).orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Permission Type:" + p)))
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
                    ZonedDateTime.now().toOffsetDateTime(),
                    permissionsFound.contains(PermissionTypes.COMPLETE),
                    permissionsFound.contains(PermissionTypes.COMPLETE_OWN),
                    permissionsFound.contains(PermissionTypes.CANCEL_OWN),
                    permissionsFound.contains(PermissionTypes.CLAIM),
                    permissionsFound.contains(PermissionTypes.UNCLAIM),
                    permissionsFound.contains(PermissionTypes.ASSIGN),
                    permissionsFound.contains(PermissionTypes.UNASSIGN),
                    permissionsFound.contains(PermissionTypes.UNCLAIM_ASSIGN),
                    permissionsFound.contains(PermissionTypes.UNASSIGN_CLAIM),
                    permissionsFound.contains(PermissionTypes.UNASSIGN_ASSIGN)
                );
            }).collect(Collectors.toSet());
    }

    private Set<TaskRoleResource> mapPermissions(
        CreateTaskRequestTask requestTask, String taskId
    ) {

        return requestTask.getPermissions().stream()
            .map(permission -> {

                final String roleName = permission.getRoleName();
                log.info("Permission found: {}", permission.getPermissions());
                final Set<PermissionTypes> permissionsFound = permission.getPermissions().stream()
                    .map(p -> PermissionTypes.from(p.value()).orElseThrow(() -> new IllegalArgumentException(
                        "Invalid Permission Type:" + p)))
                    .collect(Collectors.toSet());


                List<String> authorisations = permission.getAuthorisations();


                Integer assignmentPriority = permission.getAssignmentPriority();

                boolean autoAssignable = permission.getAutoAssignable();


                String roleCategory = permission.getRoleCategory();


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
                    taskId,
                    ZonedDateTime.now().toOffsetDateTime(),
                    permissionsFound.contains(PermissionTypes.COMPLETE),
                    permissionsFound.contains(PermissionTypes.COMPLETE_OWN),
                    permissionsFound.contains(PermissionTypes.CANCEL_OWN),
                    permissionsFound.contains(PermissionTypes.CLAIM),
                    permissionsFound.contains(PermissionTypes.UNCLAIM),
                    permissionsFound.contains(PermissionTypes.ASSIGN),
                    permissionsFound.contains(PermissionTypes.UNASSIGN),
                    permissionsFound.contains(PermissionTypes.UNCLAIM_ASSIGN),
                    permissionsFound.contains(PermissionTypes.UNASSIGN_CLAIM),
                    permissionsFound.contains(PermissionTypes.UNASSIGN_ASSIGN)
                );
            }).collect(Collectors.toSet());
    }

    private void mapVariableToTaskResourceProperty(TaskResource taskResource, String key, Object optionalValue) {
        Optional<CamundaVariableDefinition> enumKey = CamundaVariableDefinition.from(key);
        Object value = optionalValue;
        if (optionalValue instanceof Optional<?> opt) {
            value = ((Optional<?>) optionalValue).isPresent() ? opt.get() : null;
        }
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
                    taskResource.setNextHearingDate(mapDate(value));
                    break;
                case MINOR_PRIORITY:
                    setMinorPriority(value, taskResource);
                    break;
                case MAJOR_PRIORITY:
                    setMajorPriority(value, taskResource);
                    break;
                case PRIORITY_DATE:
                    taskResource.setPriorityDate(mapDate(value));
                    break;
                case DUE_DATE:
                    taskResource.setDueDateTime(mapDate(value));
                    break;
                default:
                    break;
            }
        }
    }

    public static OffsetDateTime mapDate(Object value) {
        if (Objects.isNull(value) || value instanceof String && Strings.isBlank((String) value)) {
            return null;
        }
        log.info("due date after calculation {}", value);
        LocalDateTime dateTime = LocalDateTime.parse(value.toString(), DATE_TIME_FORMATTER);
        ZoneId systemDefault = ZoneId.systemDefault();
        log.info("system default {}", systemDefault);
        OffsetDateTime dueDateTime = dateTime.atZone(systemDefault).toOffsetDateTime();
        log.info("due date during initiation {}", dueDateTime);
        return dueDateTime;
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
        if (enumKey.isPresent() && canReconfigure) {
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
                case PRIORITY_DATE:
                    taskResource.setPriorityDate(mapDate(value));
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
                    taskResource.setNextHearingDate(mapDate(value));
                    break;
                case DUE_DATE:
                    taskResource.setDueDateTime(mapDate(value));
                    break;
                case TITLE:
                    taskResource.setTitle((String) value);
                    break;
                default:
                    break;
            }
        }
    }

    protected void reconfigureAdditionalTaskAttribute(TaskResource taskResource,
                                            List<ConfigurationDmnEvaluationResponse>
                                                    configurationAdditionalAttributeDmnResponse) {
        Map<String, String> existingAdditionalProperties = taskResource.getAdditionalProperties();
        Map<String, Optional<String>> additionalProperties = new ConcurrentHashMap<>();

        //Use optionals to be able to set null values
        if (existingAdditionalProperties != null) {
            existingAdditionalProperties.entrySet().forEach(
                    e -> additionalProperties.put(e.getKey(), Optional.ofNullable(e.getValue())));
        }

        configurationAdditionalAttributeDmnResponse.stream().filter(response -> response.getCanReconfigure() != null
                && response.getCanReconfigure().getValue()).forEach(
                    response -> {
                        if (response.getName().getValue() != null && response.getValue() != null) {
                            additionalProperties.put(response.getName().getValue()
                                    .replace(ADDITIONAL_PROPERTIES_PREFIX, ""),
                                    Optional.ofNullable(response.getValue().getValue()));
                        }
                    });
        //Get the value from optional object and if not present then set to null
        if (!additionalProperties.isEmpty()) {
            taskResource.setAdditionalProperties(
                Collections.synchronizedMap(additionalProperties.entrySet().stream()
                                                .collect(
                                                    HashMap::new,
                                                    (map, value) -> map.put(
                                                        value.getKey(),
                                                        value.getValue().isPresent() ? value.getValue().get() : null
                                                    ),
                                                    HashMap::putAll
                                                )));
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

    private ExecutionTypeResource extractExecutionType(Map<CamundaVariableDefinition, Object> attributes) {
        String executionTypeName = read(attributes, EXECUTION_TYPE, null);

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

    private List<NoteResource> extractWarningNotes(Map<CamundaVariableDefinition, Object> attributes) {
        List<NoteResource> notes = null;
        WarningValues warningList = read(attributes, WARNING_LIST, null);
        if (warningList != null) {
            List<Warning> warnings = warningList.getValues();
            if (!warnings.isEmpty()) {
                notes = warnings.stream()
                    .map(warning -> new NoteResource(
                        warning.getWarningCode(),
                        "WARNING",
                        null,
                        warning.getWarningText()
                    )).toList();
            }
        }
        return notes;
    }

    private WarningValues mapNoteResourceToWarnings(List<NoteResource> notes) {

        if (notes != null) {
            List<Warning> warnings = notes.stream()
                .filter(noteResource -> "WARNING".equals(noteResource.getNoteType()))
                .map(noteResource -> new Warning(noteResource.getCode(), noteResource.getContent()))
                .toList();
            return new WarningValues(warnings);
        }
        return new WarningValues();
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> map(Map<CamundaVariableDefinition, Object> object, CamundaVariableDefinition extractor) {

        if (object == null) {
            return Optional.empty();
        }
        Object obj = object.get(extractor);
        Object value = objectMapper.convertValue(obj, extractor.getTypeReference());

        return value == null ? Optional.empty() : Optional.of((T) value);
    }
}

