package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.data.RoleAssignmentCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ADDITIONAL_PROPERTIES;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_MANAGEMENT_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WARNING_LIST;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState.CONFIGURED;

@ExtendWith(MockitoExtension.class)
class CFTTaskMapperTest {

    public static final Map<String, String> EXPECTED_ADDITIONAL_PROPERTIES = Map.of(
        "name1", "value1",
        "name2", "value2"
    );
    private final String taskId = "SOME_TASK_ID";

    @Spy
    private ObjectMapper objectMapper;

    private CFTTaskMapper cftTaskMapper;

    @BeforeEach
    void setUp() {
        cftTaskMapper = new CFTTaskMapper(objectMapper);
    }

    @Test
    void given_null_attribute_when_mapToTaskResource_then_dont_throw_exception() {
        Map<String, Object> attributes = new HashMap<>();
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        attributes.put(TaskAttributeDefinition.TASK_ASSIGNEE.value(), "someAssignee");
        attributes.put(DUE_DATE.value(), formattedDueDate);
        assertDoesNotThrow(() -> {
            cftTaskMapper.mapToTaskResource(taskId, attributes);
        });

    }

    @Test
    void should_map_task_attributes_to_cft_task() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, null);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);


        assertEquals("SOME_TASK_ID", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getDueDateTime()
        );
        assertEquals(CFTTaskState.UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertEquals("someCamundaTaskDescription", taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertEquals(5000, taskResource.getMajorPriority());
        assertEquals(500, taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getRoleCategory());
        assertEquals(true, taskResource.getHasWarnings());
        assertNull(taskResource.getAssignmentExpiry());
        assertEquals("00000", taskResource.getCaseId());
        assertEquals("someCaseType", taskResource.getCaseTypeId());
        assertEquals("someCaseName", taskResource.getCaseName());
        assertEquals("someJurisdiction", taskResource.getJurisdiction());
        assertEquals("someRegion", taskResource.getRegion());
        assertNull(taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals(EXPECTED_ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties());
        assertNull(taskResource.getBusinessContext());
        assertNull(taskResource.getTerminationReason());
        assertEquals(
            OffsetDateTime.parse(formattedCreatedDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getCreated()
        );
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertNull(taskResource.getTaskRoleResources());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getPriorityDate()
        );
    }

    @Test
    void should_map_initiation_attributes_to_cft_task() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);

        assertEquals("SOME_TASK_ID", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getDueDateTime()
        );
        assertEquals(CFTTaskState.UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertEquals("someCamundaTaskDescription", taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertEquals(5000, taskResource.getMajorPriority());
        assertEquals(500, taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getRoleCategory());
        assertEquals(true, taskResource.getHasWarnings());
        assertThat(taskResource.getNotes()).isNull();
        assertNull(taskResource.getAssignmentExpiry());
        assertEquals("00000", taskResource.getCaseId());
        assertEquals("someCaseType", taskResource.getCaseTypeId());
        assertEquals("someCaseName", taskResource.getCaseName());
        assertEquals("someJurisdiction", taskResource.getJurisdiction());
        assertEquals("someRegion", taskResource.getRegion());
        assertNull(taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals(EXPECTED_ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties());
        assertNull(taskResource.getBusinessContext());
        assertNull(taskResource.getTerminationReason());
        assertEquals(
            OffsetDateTime.parse(formattedCreatedDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getCreated()
        );
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertNull(taskResource.getTaskRoleResources());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getPriorityDate()
        );
    }

    @Test
    void should_map_task_attributes_to_cft_task_with_warnings() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributesWithWarnings(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);

        assertEquals("SOME_TASK_ID", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getDueDateTime()
        );
        assertEquals(CFTTaskState.UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertEquals("someCamundaTaskDescription", taskResource.getDescription());
        assertEquals("Code1", taskResource.getNotes().get(0).getCode());
        assertEquals("WARNING", taskResource.getNotes().get(0).getNoteType());
        assertEquals("Text1", taskResource.getNotes().get(0).getContent());
        assertEquals("Code2", taskResource.getNotes().get(1).getCode());
        assertEquals("WARNING", taskResource.getNotes().get(1).getNoteType());
        assertEquals("Text2", taskResource.getNotes().get(1).getContent());
        assertEquals(5000, taskResource.getMajorPriority());
        assertEquals(500, taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getRoleCategory());
        assertTrue(taskResource.getHasWarnings());
        assertNull(taskResource.getAssignmentExpiry());
        assertEquals("00000", taskResource.getCaseId());
        assertEquals("someCaseType", taskResource.getCaseTypeId());
        assertEquals("someCaseName", taskResource.getCaseName());
        assertEquals("someJurisdiction", taskResource.getJurisdiction());
        assertEquals("someRegion", taskResource.getRegion());
        assertNull(taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertNull(taskResource.getBusinessContext());
        assertNull(taskResource.getTerminationReason());
        assertEquals(EXPECTED_ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties());
        assertEquals(
            OffsetDateTime.parse(formattedCreatedDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getCreated()
        );
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertNull(taskResource.getTaskRoleResources());
        assertEquals(
            OffsetDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getPriorityDate()
        );
    }

    @Test
    void should_map_cftTask_to_task_with_warnings() {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributesWithWarnings(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);

        assertNotNull(task);
        assertTrue(task.getWarnings());
        assertNotNull(task.getWarningList());
        assertNotNull(task.getWarningList().getValues());
        assertEquals(2, task.getWarningList().getValues().size());

    }

    @Test
    void should_map_configuration_attributes() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());
        mappedValues.put(TASK_TYPE.value(), "someTaskType");
        mappedValues.put(AUTO_ASSIGNED.value(), false);
        mappedValues.put(JURISDICTION.value(), "IA");
        mappedValues.put(CASE_NAME.value(), "Bob Smith");
        mappedValues.put(CASE_TYPE_ID.value(), "someCaseTypeId");
        mappedValues.put(EXECUTION_TYPE.value(), "MANUAL");
        mappedValues.put(LOCATION.value(), "someStaffLocationId");
        mappedValues.put(LOCATION_NAME.value(), "someStaffLocationName");
        mappedValues.put(REGION.value(), "1");
        mappedValues.put(SECURITY_CLASSIFICATION.value(), "PUBLIC");
        mappedValues.put(TASK_SYSTEM.value(), "SELF");
        mappedValues.put(TITLE.value(), "someTitle");
        mappedValues.put(HAS_WARNINGS.value(), false);
        mappedValues.put(CASE_MANAGEMENT_CATEGORY.value(), "someCaseCategory");
        mappedValues.put(ROLE_CATEGORY.value(), "LEGAL_OPERATIONS");
        mappedValues.put(ADDITIONAL_PROPERTIES.value(), writeValueAsString(EXPECTED_ADDITIONAL_PROPERTIES));
        String nextHearingId = "nextHearingId";
        mappedValues.put(NEXT_HEARING_ID.value(), nextHearingId);
        String nextHearingDate = "2021-05-13T20:15";
        mappedValues.put(NEXT_HEARING_DATE.value(), nextHearingDate);
        String priorityDate = "2021-05-12T20:15";
        mappedValues.put(PRIORITY_DATE.value(), priorityDate);
        String dueDate = "2021-05-10T20:15";
        mappedValues.put(DUE_DATE.value(), dueDate);
        mappedValues.put(MAJOR_PRIORITY.value(), 5000);
        mappedValues.put(MINOR_PRIORITY.value(), 500);

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );


        assertEquals("SOME_TASK_ID", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertNull(taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertNull(taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertEquals("LEGAL_OPERATIONS", taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertNull(taskResource.getAssignmentExpiry());
        assertEquals("someCaseId", taskResource.getCaseId());
        assertEquals("someCaseTypeId", taskResource.getCaseTypeId());
        assertEquals("Bob Smith", taskResource.getCaseName());
        assertEquals("IA", taskResource.getJurisdiction());
        assertEquals("1", taskResource.getRegion());
        assertNull(taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals("someCaseCategory", taskResource.getCaseCategory());
        assertEquals(EXPECTED_ADDITIONAL_PROPERTIES, taskResource.getAdditionalProperties());
        assertEquals(nextHearingId, taskResource.getNextHearingId());
        assertNull(taskResource.getBusinessContext());
        assertNull(taskResource.getTerminationReason());
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertEquals(emptySet(), taskResource.getTaskRoleResources());
        assertEquals(5000, taskResource.getMajorPriority());
        assertEquals(500, taskResource.getMinorPriority());
        assertEquals(CFTTaskMapper.mapDate(nextHearingDate), taskResource.getNextHearingDate());
        assertEquals(CFTTaskMapper.mapDate(priorityDate), taskResource.getPriorityDate());
        assertEquals(CFTTaskMapper.mapDate(dueDate), taskResource.getDueDateTime());
    }


    @Test
    void should_map_configuration_attributes_when_skeleton_fields_changed() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(CamundaVariableDefinition.CASE_ID.value(), "otherCaseId");
        mappedValues.put(CamundaVariableDefinition.TASK_ID.value(), "otherTaskId");
        mappedValues.put(CamundaVariableDefinition.TASK_NAME.value(), "otherTaskName");

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );


        assertEquals("otherCaseId", taskResource.getCaseId());
        assertEquals("otherTaskId", taskResource.getTaskId());
        assertEquals("otherTaskName", taskResource.getTaskName());

    }

    @Test
    void should_map_configuration_attributes_with_permissions() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());
        mappedValues.put(TASK_TYPE.value(), "someTaskType");
        mappedValues.put(AUTO_ASSIGNED.value(), false);
        mappedValues.put(JURISDICTION.value(), "IA");
        mappedValues.put(CASE_NAME.value(), "Bob Smith");
        mappedValues.put(CASE_TYPE_ID.value(), "someCaseTypeId");
        mappedValues.put(EXECUTION_TYPE.value(), "MANUAL");
        mappedValues.put(LOCATION.value(), "someStaffLocationId");
        mappedValues.put(LOCATION_NAME.value(), "someStaffLocationName");
        mappedValues.put(REGION.value(), "1");
        mappedValues.put(SECURITY_CLASSIFICATION.value(), "PUBLIC");
        mappedValues.put(TASK_SYSTEM.value(), "SELF");
        mappedValues.put(TITLE.value(), "someTitle");
        mappedValues.put(HAS_WARNINGS.value(), false);
        mappedValues.put(CASE_MANAGEMENT_CATEGORY.value(), "someCaseCategory");
        mappedValues.put(NEXT_HEARING_ID.value(), null);
        mappedValues.put(NEXT_HEARING_DATE.value(), null);
        List<PermissionsDmnEvaluationResponse> permissionsDmnEvaluationResponses =
            asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            );

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, emptyList(), permissionsDmnEvaluationResponses)
        );


        assertEquals("SOME_TASK_ID", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertNull(taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertNull(taskResource.getMajorPriority());
        assertNull(taskResource.getMinorPriority());
        assertNull(taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertNull(taskResource.getAssignmentExpiry());
        assertEquals("someCaseId", taskResource.getCaseId());
        assertEquals("someCaseTypeId", taskResource.getCaseTypeId());
        assertEquals("Bob Smith", taskResource.getCaseName());
        assertEquals("IA", taskResource.getJurisdiction());
        assertEquals("1", taskResource.getRegion());
        assertNull(taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals("someCaseCategory", taskResource.getCaseCategory());
        assertNull(taskResource.getBusinessContext());
        assertNull(taskResource.getTerminationReason());
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertNotNull(taskResource.getTaskRoleResources());
        List<TaskRoleResource> roleResourcesList = new ArrayList<>(taskResource.getTaskRoleResources());

        assertEquals("senior-tribunal-caseworker", roleResourcesList.get(0).getRoleName());
        assertEquals(true, roleResourcesList.get(0).getRead());
        assertEquals(true, roleResourcesList.get(0).getOwn());
        assertEquals(false, roleResourcesList.get(0).getExecute());
        assertEquals(true, roleResourcesList.get(0).getManage());
        assertEquals(true, roleResourcesList.get(0).getCancel());
        assertEquals(true, roleResourcesList.get(0).getRefer());
        assertEquals(false, roleResourcesList.get(0).getClaim());
        assertEquals(false, roleResourcesList.get(0).getAssign());
        assertEquals(false, roleResourcesList.get(0).getUnassign());
        assertEquals(false, roleResourcesList.get(0).getUnassignAssign());
        assertEquals(false, roleResourcesList.get(0).getComplete());
        assertEquals(false, roleResourcesList.get(0).getCompleteOwn());
        assertEquals(false, roleResourcesList.get(0).getCancelOwn());
        assertEquals(false, roleResourcesList.get(0).getUnassignClaim());
        assertEquals(false, roleResourcesList.get(0).getUnclaim());
        assertEquals(false, roleResourcesList.get(0).getUnclaimAssign());
        assertArrayEquals(new String[]{}, roleResourcesList.get(0).getAuthorizations());
        assertEquals("tribunal-caseworker", roleResourcesList.get(1).getRoleName());
        assertEquals(true, roleResourcesList.get(1).getRead());
        assertEquals(true, roleResourcesList.get(1).getOwn());
        assertEquals(false, roleResourcesList.get(1).getExecute());
        assertEquals(true, roleResourcesList.get(1).getManage());
        assertEquals(true, roleResourcesList.get(1).getCancel());
        assertEquals(true, roleResourcesList.get(1).getRefer());
        assertEquals(false, roleResourcesList.get(0).getClaim());
        assertEquals(false, roleResourcesList.get(0).getAssign());
        assertEquals(false, roleResourcesList.get(0).getUnassign());
        assertEquals(false, roleResourcesList.get(0).getUnassignAssign());
        assertEquals(false, roleResourcesList.get(0).getComplete());
        assertEquals(false, roleResourcesList.get(0).getCompleteOwn());
        assertEquals(false, roleResourcesList.get(0).getCancelOwn());
        assertEquals(false, roleResourcesList.get(0).getUnassignClaim());
        assertEquals(false, roleResourcesList.get(0).getUnclaim());
        assertEquals(false, roleResourcesList.get(0).getUnclaimAssign());
        assertArrayEquals(new String[]{"IA", "WA"}, roleResourcesList.get(1).getAuthorizations());
        assertNull(taskResource.getNextHearingId());
        assertNull(taskResource.getNextHearingDate());
    }

    @Test
    void should_map_configuration_attributes_with_different_permission_types() {

        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(AUTO_ASSIGNED.value(), true);
        mappedValues.put(ASSIGNEE.value(), "someAssignee");
        mappedValues.put(JURISDICTION.value(), "IA");
        mappedValues.put(HAS_WARNINGS.value(), true);
        mappedValues.put(NEXT_HEARING_ID.value(), "");
        mappedValues.put(NEXT_HEARING_DATE.value(), "");
        List<PermissionsDmnEvaluationResponse> permissionsDmnEvaluationResponses =
            asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("  Read , Refer , Own , Manage , Cancel   "),
                    stringValue("IA,WA"),
                    integerValue(2),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue("categoryB")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    integerValue(1),
                    booleanValue(false),
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue("categoryA,categoryF")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("some-caseworker"),
                    stringValue("Read"),
                    null,
                    integerValue(null),
                    booleanValue(null),
                    stringValue(null),
                    stringValue(null)
                )
            );

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, emptyList(), permissionsDmnEvaluationResponses)
        );


        assertTrue(taskResource.getAutoAssigned());
        assertNotNull(taskResource.getAssignee());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals("someCaseId", taskResource.getCaseId());
        assertTrue(taskResource.getHasWarnings());

        List<TaskRoleResource> actualRoleResources = new ArrayList<>(taskResource.getTaskRoleResources());

        assertNotNull(actualRoleResources);
        assertEquals(3, actualRoleResources.size());

        assertEquals("senior-tribunal-caseworker", actualRoleResources.get(0).getRoleName());
        assertEquals(0, actualRoleResources.get(0).getAuthorizations().length);
        assertEquals(1, actualRoleResources.get(0).getAssignmentPriority());
        assertFalse(actualRoleResources.get(0).getAutoAssignable());
        assertEquals("LEGAL_OPERATIONS", actualRoleResources.get(0).getRoleCategory());

        assertEquals("some-caseworker", actualRoleResources.get(1).getRoleName());
        assertEquals(0, actualRoleResources.get(1).getAuthorizations().length);
        assertNull(actualRoleResources.get(1).getAssignmentPriority());
        assertFalse(actualRoleResources.get(1).getAutoAssignable());
        assertNull(actualRoleResources.get(1).getRoleCategory());

        assertEquals("tribunal-caseworker", actualRoleResources.get(2).getRoleName());
        assertEquals(2, actualRoleResources.get(2).getAuthorizations().length);
        assertArrayEquals(
            new String[]{"IA", "WA"},
            actualRoleResources.get(2).getAuthorizations()
        );
        assertEquals(2, actualRoleResources.get(2).getAssignmentPriority());
        assertTrue(actualRoleResources.get(2).getAutoAssignable());
        assertEquals("LEGAL_OPERATIONS", actualRoleResources.get(2).getRoleCategory());
        assertNull(taskResource.getNextHearingId());
        assertNull(taskResource.getNextHearingDate());
    }

    @Test
    void should_throw_exception_when_permission_type_enum_is_not_mapped() {

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(CASE_ID.value(), "otherCaseId");
        mappedValues.put(CamundaVariableDefinition.TASK_ID.value(), "otherTaskId");
        mappedValues.put(CamundaVariableDefinition.TASK_NAME.value(), "otherTaskName");

        List<PermissionsDmnEvaluationResponse> permissionsDmnEvaluationResponses =
            asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue(" Read , Refer,Own,Manage, somePermissionType"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS"),
                    stringValue(null)
                )
            );

        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        assertThatThrownBy(() -> cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, emptyList(), permissionsDmnEvaluationResponses
            )
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid Permission Type:somePermissionType")
            .hasNoCause();
    }

    @Test
    void should_throw_exception_when_execution_type_enum_is_not_mapped() {

        Map<String, Object> attributes = Map.of(
            CamundaVariableDefinition.EXECUTION_TYPE.value(), "someExecutionType");

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ExecutionTypeName value: 'someExecutionType' could not be mapped to ExecutionType enum")
            .hasNoCause();
    }

    @Test
    void should_throw_exception_when_no_due_date() {

        Map<String, Object> attributes = Map.of(
            CamundaVariableDefinition.TASK_TYPE.value(), "aTaskType",
            CamundaVariableDefinition.TASK_NAME.value(), "aTaskName",
            TASK_CASE_ID.value(), "someCaseId"
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("DUE_DATE must not be null")
            .hasNoCause();
    }

    @Test
    void should_throw_exception_when_task_system_enum_is_not_mapped() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = Map.of(
            CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL",
            CamundaVariableDefinition.TASK_SYSTEM.value(), "someTaskSystem",
            DUE_DATE.value(), formattedDueDate
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(
                "Cannot deserialize value of type `uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem` "
                    + "from String \"someTaskSystem\": not one of the values accepted for Enum class: [CTSC, SELF]")
            .hasCauseInstanceOf(InvalidFormatException.class);

    }

    @Test
    void should_throw_exception_when_security_classification_enum_is_not_mapped() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = Map.of(
            CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL",
            CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF",
            CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "someInvalidEnumValue",
            DUE_DATE.value(), formattedDueDate
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(
                "Cannot deserialize value of type "
                + "`uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification` "
                + "from String \"someInvalidEnumValue\": not one of the values accepted for Enum class: "
                + "[PUBLIC, RESTRICTED, PRIVATE]")
            .hasCauseInstanceOf(InvalidFormatException.class);

    }

    @Test
    void should_throw_exception_when_additional_properties_is_not_valid() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(ADDITIONAL_PROPERTIES.value(), "invalid_prop");

        assertThatThrownBy(() -> cftTaskMapper
            .mapConfigurationAttributes(skeletonTask, new TaskConfigurationResults(mappedValues)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Additional Properties mapping issue.")
            .hasCauseInstanceOf(JsonParseException.class);
    }

    @Test
    void should_return_null_when_additional_properties_is_null() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(ADDITIONAL_PROPERTIES.value(), null);

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );

        assertNull(taskResource.getAdditionalProperties());
    }

    @Test
    void should_return_role_assignment_id_when_additional_properties_have_role_assignment_id() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(ADDITIONAL_PROPERTIES.value(), writeValueAsString(Map.of("roleAssignmentId", "1234567890")));

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );

        assertNotNull(taskResource.getAdditionalProperties());
        assertEquals(taskResource.getAdditionalProperties().get("roleAssignmentId"), "1234567890");
    }

    @Test
    void should_map_configuration_attributes_when_work_type_is_null() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(WORK_TYPE.value(), null);

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );

        assertNotNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getWorkTypeResource().getId());
        assertEquals(emptySet(), taskResource.getTaskRoleResources());
    }

    @Test
    void should_map_task_resource_to_task_when_work_type_is_not_null() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributesWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER,
                PermissionTypes.CLAIM,
                PermissionTypes.ASSIGN,
                PermissionTypes.UNASSIGN,
                PermissionTypes.UNASSIGN_ASSIGN,
                PermissionTypes.COMPLETE,
                PermissionTypes.COMPLETE_OWN,
                PermissionTypes.CANCEL_OWN,
                PermissionTypes.UNASSIGN_CLAIM,
                PermissionTypes.UNCLAIM,
                PermissionTypes.UNCLAIM_ASSIGN
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);


        assertEquals("someWorkType", task.getWorkTypeId());

        assertNotNull(taskResource.getWorkTypeResource());
        assertEquals("someWorkType", taskResource.getWorkTypeResource().getId());
    }

    @Test
    void should_map_task_resource_to_task_when_work_type_is_null() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, null);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);


        assertNull(task.getWorkTypeId());

        assertNull(taskResource.getWorkTypeResource());
    }

    @Test
    void should_map_task_resource_to_task_when_work_type_not_exists() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes
            = getDefaultAttributesWithoutWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setWorkTypeResource(null);

        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);


        assertNull(task.getWorkTypeId());

        AssertionsForClassTypes.assertThatThrownBy(() -> taskResource.getWorkTypeResource().getId())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_map_task_resource_to_task_when_security_classification_not_exists() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes =
            getDefaultAttributesWithoutWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setSecurityClassification(null);

        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);


        assertNull(task.getSecurityClassification());

        AssertionsForClassTypes.assertThatThrownBy(() -> taskResource.getSecurityClassification()
                .getSecurityClassification())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_map_task_resource_to_task_when_task_system_not_exists() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes =
            getDefaultAttributesWithoutWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setTaskSystem(null);

        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);
        assertNull(task.getTaskSystem());

        AssertionsForClassTypes.assertThatThrownBy(() -> taskResource.getTaskSystem().getValue())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_map_task_resource_to_task_when_execution_type_code_not_exists() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes =
            getDefaultAttributesWithoutWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setExecutionTypeCode(null);

        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);
        assertNull(task.getExecutionType());

        AssertionsForClassTypes.assertThatThrownBy(() -> taskResource.getExecutionTypeCode().getExecutionCode())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_exception_when_map_task_resource_to_task_and_due_date_is_null() {
        Map<String, Object> attributes = getDefaultAttributesWithoutDueDate();

        AssertionsForClassTypes.assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("DUE_DATE must not be null");
    }

    @Test
    void should_default_created_date_when_not_provided() {
        String formattedCreatedDate = null;
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, null);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        assertNotNull(taskResource.getCreated());
        assertNotNull(taskResource.getDueDateTime());
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );

        assertDoesNotThrow(() -> cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion));
    }

    @Test
    void should_throw_exception_when_map_to_task_and_due_date_is_null() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, null);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        assertNotNull(taskResource.getDueDateTime());

        //given
        taskResource.setDueDateTime(null);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        assertThatThrownBy(() -> cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_map_task_resource_to_task_with_permissions_empty() {

        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            false,
            false,
            false,
            false,
            false,
            false,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
        TaskResource taskResource = createTaskResourceWithRoleResource(roleResource);
        taskResource.setReconfigureRequestTime(OffsetDateTime.now());
        taskResource.setLastReconfigurationTime(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("someAction");
        taskResource.setLastUpdatedUser("someUser");
        OffsetDateTime lastUpdatedTimeStamp = OffsetDateTime.now();
        taskResource.setLastUpdatedTimestamp(lastUpdatedTimeStamp);

        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, new HashSet<>());


        assertEquals("taskId", task.getId());
        assertEquals("aTaskName", task.getName());
        assertEquals("startAppeal", task.getType());
        assertNotNull(task.getDueDate());
        assertEquals("completed", task.getTaskState());
        assertEquals("SELF", task.getTaskSystem());
        assertEquals("PUBLIC", task.getSecurityClassification());
        assertEquals("title", task.getTaskTitle());
        assertEquals("someAssignee", task.getAssignee());
        assertFalse(task.isAutoAssigned());
        assertEquals("Manual", task.getExecutionType());
        assertEquals("IA", task.getJurisdiction());
        assertEquals("1623278362430412", task.getCaseId());
        assertEquals("Asylum", task.getCaseTypeId());
        assertEquals("TestCase", task.getCaseName());
        assertEquals("1", task.getRegion());
        assertEquals("765324", task.getLocation());
        assertEquals("Taylor House", task.getLocationName());
        assertEquals("caseCategory", taskResource.getCaseCategory());
        assertEquals("caseCategory", task.getCaseManagementCategory());
        assertEquals(false, task.getWarnings());
        assertEquals(emptyList(), task.getWarningList().getValues());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
        assertNotNull(task.getPermissions());
        assertTrue(task.getPermissions().getValues().isEmpty());
        assertNotNull(task.getReconfigureRequestTime());
        assertNotNull(task.getLastReconfigurationTime());
    }

    @Test
    void should_map_task_resource_to_task_with_permissions() {

        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
        TaskResource taskResource = createTaskResourceWithRoleResource(roleResource);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER,
                PermissionTypes.COMPLETE,
                PermissionTypes.COMPLETE_OWN,
                PermissionTypes.CANCEL_OWN,
                PermissionTypes.CLAIM,
                PermissionTypes.UNCLAIM,
                PermissionTypes.ASSIGN,
                PermissionTypes.UNASSIGN,
                PermissionTypes.UNCLAIM_ASSIGN,
                PermissionTypes.UNASSIGN_CLAIM,
                PermissionTypes.UNASSIGN_ASSIGN
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);

        assertEquals("taskId", task.getId());
        assertEquals("aTaskName", task.getName());
        assertEquals("startAppeal", task.getType());
        assertNotNull(task.getDueDate());
        assertEquals("completed", task.getTaskState());
        assertEquals("SELF", task.getTaskSystem());
        assertEquals("PUBLIC", task.getSecurityClassification());
        assertEquals("title", task.getTaskTitle());
        assertEquals("someAssignee", task.getAssignee());
        assertFalse(task.isAutoAssigned());
        assertEquals("Manual", task.getExecutionType());
        assertEquals("IA", task.getJurisdiction());
        assertEquals("1623278362430412", task.getCaseId());
        assertEquals("Asylum", task.getCaseTypeId());
        assertEquals("TestCase", task.getCaseName());
        assertEquals("1", task.getRegion());
        assertEquals("765324", task.getLocation());
        assertEquals("Taylor House", task.getLocationName());
        assertEquals("caseCategory", taskResource.getCaseCategory());
        assertEquals("caseCategory", task.getCaseManagementCategory());
        assertEquals(false, task.getWarnings());
        assertEquals(emptyList(), task.getWarningList().getValues());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
        assertNotNull(task.getPermissions());
        assertFalse(task.getPermissions().getValues().isEmpty());
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.READ));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.OWN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.MANAGE));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.EXECUTE));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.CANCEL));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.REFER));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.COMPLETE));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.COMPLETE_OWN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.CANCEL_OWN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.CLAIM));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.UNCLAIM));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.ASSIGN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.UNASSIGN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.UNASSIGN_CLAIM));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.UNASSIGN_ASSIGN));
        assertNull(task.getReconfigureRequestTime());
        assertNull(task.getLastReconfigurationTime());
    }

    @Test
    void should_map_task_resource_to_task() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);


        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate, null);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);


        assertEquals("SOME_TASK_ID", task.getId());
        assertEquals("someCamundaTaskName", task.getName());
        assertEquals("someTaskType", task.getType());
        assertEquals(
            ZonedDateTime.parse(formattedDueDate, CAMUNDA_DATA_TIME_FORMATTER),
            task.getDueDate()
        );
        assertEquals(UNCONFIGURED.getValue().toLowerCase(Locale.ROOT), task.getTaskState());
        assertEquals(TaskSystem.SELF.getValue(), task.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC.getSecurityClassification(), task.getSecurityClassification());
        assertEquals("someTitle", task.getTaskTitle());
        assertEquals("someAssignee", task.getAssignee());
        assertFalse(task.isAutoAssigned());
        assertEquals("Manual", task.getExecutionType());
        assertEquals("00000", task.getCaseId());
        assertEquals("someCaseType", task.getCaseTypeId());
        assertEquals("someCaseName", task.getCaseName());
        assertEquals("someJurisdiction", task.getJurisdiction());
        assertEquals("someRegion", task.getRegion());
        assertEquals("someStaffLocationId", task.getLocation());
        assertEquals("someStaffLocationName", task.getLocationName());
        assertEquals("someCaseCategory", taskResource.getCaseCategory());
        assertEquals(true, task.getWarnings());
        assertEquals(emptyList(), task.getWarningList().getValues());
        assertEquals("someCaseCategory", task.getCaseManagementCategory());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
    }

    @Test
    void should_set_priority_date_different_from_priority_date() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        ZonedDateTime priorityDate = createdDate.plusDays(3);
        String formattedPriorityDate = CAMUNDA_DATA_TIME_FORMATTER.format(priorityDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate,
                                                              formattedPriorityDate
        );

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);

        assertNotEquals(task.getDueDate(), task.getPriorityDate());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
    }

    @Test
    void should_set_priority_date_null_no_error() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate,
                                                              null
        );

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setPriorityDate(null);
        Set<PermissionTypes> permissionsUnion = new HashSet<>(
            asList(
                PermissionTypes.READ,
                PermissionTypes.OWN,
                PermissionTypes.MANAGE,
                PermissionTypes.EXECUTE,
                PermissionTypes.CANCEL,
                PermissionTypes.REFER
            )
        );
        Task task = cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnion);

        assertNotEquals(task.getDueDate(), task.getPriorityDate());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
    }

    @Test
    void should_convert_task_resource_to_map_object() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = getDefaultAttributesWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Map<String, Object> taskAttributes = cftTaskMapper.getTaskAttributes(taskResource);

        assertThat(taskAttributes).size().isEqualTo(42);
    }

    @Test
    void should_extract_permission_union_all_true() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_all_true_with_non_granular_permission() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            false,
            true
        );
        TaskResource taskResource = mock(TaskResource.class);
        when(taskResource.getCaseId()).thenReturn("CASE_ID");
        taskRoleResource.setTaskResource(taskResource);
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));

        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE));
        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_ASSIGN));
    }

    @Test
    void should_extract_permission_union_all_true_with_granular_permission() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            false,
            true
        );
        TaskResource taskResource = mock(TaskResource.class);
        when(taskResource.getCaseId()).thenReturn("CASE_ID");
        taskRoleResource.setTaskResource(taskResource);
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, true);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));

        assertTrue(permissionsUnion.contains(PermissionTypes.COMPLETE));
        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL_OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM));
        assertTrue(permissionsUnion.contains(PermissionTypes.ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_CLAIM));
        assertTrue(permissionsUnion.contains(PermissionTypes.UNASSIGN_ASSIGN));
    }

    @Test
    void should_extract_union_and_map_to_task() {


        TaskRoleResource taskRoleResource1 = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            false,
            false,
            false,
            new String[]{},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            false,
            true
        );
        TaskRoleResource taskRoleResource2 = new TaskRoleResource(
            "senior-tribunal-caseworker",
            false,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            false,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            true
        );

        Set<TaskRoleResource> taskRoleResources = new HashSet<>(asList(taskRoleResource1, taskRoleResource2));
        TaskResource taskResource = new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            taskRoleResources,
            "caseCategory",
            EXPECTED_ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );

        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Task mappedTask = cftTaskMapper.mapToTaskAndExtractPermissionsUnion(taskResource, roleAssignments, false);

        assertNotNull(mappedTask);

        TaskPermissions taskPermissions = mappedTask.getPermissions();
        assertNotNull(taskPermissions.getValues());

        Set<PermissionTypes> permissionsUnion = taskPermissions.getValues();
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertFalse(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertFalse(permissionsUnion.contains(PermissionTypes.REFER));

        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE));
        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_ASSIGN));
    }

    @Test
    void should_extract_union_and_map_to_task_granular_permission() {

        TaskRoleResource taskRoleResource1 = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            false,
            false,
            false,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            false,
            false,
            true,
            false,
            true,
            false,
            false,
            false,
            true
        );
        TaskRoleResource taskRoleResource2 = new TaskRoleResource(
            "senior-tribunal-caseworker",
            false,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true,
            "JUDICIAL",
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            false,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            true
        );

        Set<TaskRoleResource> taskRoleResources = new HashSet<>(asList(taskRoleResource1, taskRoleResource2));
        TaskResource taskResource = new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            taskRoleResources,
            "caseCategory",
            EXPECTED_ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );

        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Task mappedTask = cftTaskMapper.mapToTaskAndExtractPermissionsUnion(taskResource, roleAssignments, true);

        assertNotNull(mappedTask);

        TaskPermissions taskPermissions = mappedTask.getPermissions();
        assertNotNull(taskPermissions.getValues());

        Set<PermissionTypes> permissionsUnion = taskPermissions.getValues();
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertFalse(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertFalse(permissionsUnion.contains(PermissionTypes.REFER));

        assertTrue(permissionsUnion.contains(PermissionTypes.COMPLETE));
        assertFalse(permissionsUnion.contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL_OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.CLAIM));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM));
        assertTrue(permissionsUnion.contains(PermissionTypes.ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(permissionsUnion.contains(PermissionTypes.UNASSIGN_CLAIM));
        assertTrue(permissionsUnion.contains(PermissionTypes.UNASSIGN_ASSIGN));
    }

    @Test
    void should_extract_permission_union_read_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            false,
            true,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertFalse(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_own_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            false,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertFalse(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_execute_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_manage_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            false,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertFalse(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_cancel_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            false,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_extract_permission_union_refer_false() {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            false,
            new String[]{},
            0,
            true
        );
        taskRoleResource.setTaskResource(createTaskResource());
        Set<TaskRoleResource> taskRoleResources = new HashSet<>(singletonList(taskRoleResource));
        List<RoleAssignment> roleAssignments = singletonList(RoleAssignmentCreator.aRoleAssignment().build());

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertTrue(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertFalse(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void give_multiple_role_should_extract_permission_union_correctly_when_only_one_role_assignment() {

        TaskRoleResource taskRoleResource1 = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            false,
            false,
            false,
            new String[]{},
            0,
            true
        );
        taskRoleResource1.setTaskResource(createTaskResource());
        TaskRoleResource taskRoleResource2 = new TaskRoleResource(
            "senior-tribunal-caseworker",
            false,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource2.setTaskResource(createTaskResource());

        Set<TaskRoleResource> taskRoleResources = new HashSet<>(asList(taskRoleResource1, taskRoleResource2));
        List<RoleAssignment> roleAssignments = singletonList(
            RoleAssignmentCreator.aRoleAssignment().roleName("tribunal-caseworker").build()
        );

        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertEquals(2, permissionsUnion.size());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertFalse(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertFalse(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertFalse(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void give_multiple_role_should_extract_permission_union_correctly_when_only_multiple_role_assignment() {

        TaskRoleResource taskRoleResource1 = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            false,
            false,
            false,
            new String[]{},
            0,
            true
        );
        taskRoleResource1.setTaskResource(createTaskResource());
        TaskRoleResource taskRoleResource2 = new TaskRoleResource(
            "senior-tribunal-caseworker",
            false,
            true,
            false,
            true,
            true,
            true,
            new String[]{},
            0,
            true
        );
        taskRoleResource1.setTaskResource(createTaskResource());

        Set<TaskRoleResource> taskRoleResources = new HashSet<>(asList(taskRoleResource1, taskRoleResource2));
        List<RoleAssignment> roleAssignments = asList(
            RoleAssignmentCreator.aRoleAssignment().roleName("tribunal-caseworker").build(),
            RoleAssignmentCreator.aRoleAssignment().roleName("senior-tribunal-caseworker").build()
        );
        Set<PermissionTypes> permissionsUnion =
            cftTaskMapper.extractUnionOfPermissionsForUser(taskRoleResources, roleAssignments, false);

        assertFalse(permissionsUnion.isEmpty());
        assertEquals(5, permissionsUnion.size());
        assertTrue(permissionsUnion.contains(PermissionTypes.READ));
        assertTrue(permissionsUnion.contains(PermissionTypes.OWN));
        assertFalse(permissionsUnion.contains(PermissionTypes.EXECUTE));
        assertTrue(permissionsUnion.contains(PermissionTypes.MANAGE));
        assertTrue(permissionsUnion.contains(PermissionTypes.CANCEL));
        assertTrue(permissionsUnion.contains(PermissionTypes.REFER));
    }

    @Test
    void should_map_task_role_permissions() {
        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );

        final TaskRolePermissions taskRolePermissions = cftTaskMapper.mapToTaskRolePermissions(roleResource, false);

        assertEquals("tribunal-caseworker", taskRolePermissions.getRoleName());
        assertEquals("JUDICIAL", taskRolePermissions.getRoleCategory());
        assertNotNull(taskRolePermissions.getPermissions());
        assertFalse(taskRolePermissions.getPermissions().isEmpty());
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.READ));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.MANAGE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.EXECUTE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.REFER));

        assertFalse(taskRolePermissions.getAuthorisations().isEmpty());
        assertTrue(taskRolePermissions.getAuthorisations().contains("SPECIFIC"));
        assertTrue(taskRolePermissions.getAuthorisations().contains("STANDARD"));
    }

    @Test
    void should_map_task_role_permissions_when_authorisations_are_empty() {
        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );

        final TaskRolePermissions taskRolePermissions = cftTaskMapper.mapToTaskRolePermissions(roleResource, false);

        assertEquals("tribunal-caseworker", taskRolePermissions.getRoleName());
        assertEquals("JUDICIAL", taskRolePermissions.getRoleCategory());
        assertNotNull(taskRolePermissions.getPermissions());
        assertFalse(taskRolePermissions.getPermissions().isEmpty());
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.READ));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.MANAGE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.EXECUTE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.REFER));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL_OWN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.CLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.ASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_CLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_ASSIGN));

        assertTrue(taskRolePermissions.getAuthorisations().isEmpty());
    }

    @Test
    void should_map_task_role_permissions_when_authorisations_are_null() {
        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            null,
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            false, false, false, false, false, false, false, false, false, false
        );

        final TaskRolePermissions taskRolePermissions = cftTaskMapper.mapToTaskRolePermissions(roleResource, false);

        assertEquals("tribunal-caseworker", taskRolePermissions.getRoleName());
        assertEquals("JUDICIAL", taskRolePermissions.getRoleCategory());
        assertNotNull(taskRolePermissions.getPermissions());
        assertFalse(taskRolePermissions.getPermissions().isEmpty());
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.READ));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.MANAGE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.EXECUTE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.REFER));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE_OWN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL_OWN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.CLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.ASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM_ASSIGN));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_CLAIM));
        assertFalse(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_ASSIGN));

        assertTrue(taskRolePermissions.getAuthorisations().isEmpty());
    }

    @Test
    void should_map_configuration_attributes_description() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(CamundaVariableDefinition.CASE_ID.value(), "otherCaseId");
        mappedValues.put(CamundaVariableDefinition.TASK_ID.value(), "otherTaskId");
        mappedValues.put(CamundaVariableDefinition.TASK_NAME.value(), "otherTaskName");
        mappedValues.put(CamundaVariableDefinition.DESCRIPTION.value(), "aDescription");

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );


        assertEquals("otherCaseId", taskResource.getCaseId());
        assertEquals("otherTaskId", taskResource.getTaskId());
        assertEquals("otherTaskName", taskResource.getTaskName());
        assertEquals("aDescription", taskResource.getDescription());

    }

    @Test
    void should_map_configuration_attributes_priority_from_string() {
        TaskResource skeletonTask = new TaskResource(
            taskId,
            "someCamundaTaskName",
            "someTaskType",
            UNCONFIGURED,
            "someCaseId"
        );

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(CamundaVariableDefinition.CASE_ID.value(), "otherCaseId");
        mappedValues.put(CamundaVariableDefinition.TASK_ID.value(), "otherTaskId");
        mappedValues.put(CamundaVariableDefinition.TASK_NAME.value(), "otherTaskName");
        mappedValues.put(CamundaVariableDefinition.DESCRIPTION.value(), "aDescription");
        mappedValues.put(PRIORITY_DATE.value(), "2022-05-09T20:15");
        mappedValues.put(MAJOR_PRIORITY.value(), "5000");
        mappedValues.put(MINOR_PRIORITY.value(), "500");

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues)
        );


        assertEquals("otherCaseId", taskResource.getCaseId());
        assertEquals("otherTaskId", taskResource.getTaskId());
        assertEquals("otherTaskName", taskResource.getTaskName());
        assertEquals("aDescription", taskResource.getDescription());

    }

    @Test
    void can_reconfigure_a_task_with_data_from_configuration_DMN_when_canReconfigure_true() {

        TaskResource taskResource = createTaskResource();

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );

        TaskResource reconfiguredTaskResource = cftTaskMapper
            .reconfigureTaskResourceFromDmnResults(taskResource, results);
        assertEquals(taskResource.getTitle(), reconfiguredTaskResource.getTitle());
        assertEquals(taskResource.getDescription(), reconfiguredTaskResource.getDescription());
        assertEquals(taskResource.getCaseName(), reconfiguredTaskResource.getCaseName());
        assertEquals(taskResource.getRegion(), reconfiguredTaskResource.getRegion());
        assertEquals("512401", reconfiguredTaskResource.getLocation());
        assertEquals("Manchester", reconfiguredTaskResource.getLocationName());
        assertEquals(taskResource.getCaseCategory(), reconfiguredTaskResource.getCaseCategory());
        assertEquals(
            taskResource.getWorkTypeResource().getId(),
            reconfiguredTaskResource.getWorkTypeResource().getId()
        );
        assertEquals(taskResource.getRoleCategory(), reconfiguredTaskResource.getRoleCategory());
        assertEquals(taskResource.getPriorityDate(), reconfiguredTaskResource.getPriorityDate());
        assertEquals(1, reconfiguredTaskResource.getMinorPriority());
        assertEquals(1, reconfiguredTaskResource.getMajorPriority());
        assertEquals("nextHearingId1", reconfiguredTaskResource.getNextHearingId());
        assertEquals(taskResource.getNextHearingDate(), reconfiguredTaskResource.getNextHearingDate());
        LocalDateTime localDateTime = LocalDateTime.of(2021, 5, 9, 20, 15, 0, 0);
        assertEquals(
            localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime(),
            reconfiguredTaskResource.getDueDateTime()
        );
    }

    @Test
    void can_not_reconfigure_a_task_with_data_from_configuration_DMN_when_canReconfigure_false() {

        TaskResource taskResource = createTaskResource();

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(false),
            permissionsResponse()
        );

        TaskResource reconfiguredTaskResource = cftTaskMapper
            .reconfigureTaskResourceFromDmnResults(taskResource, results);
        assertEquals(taskResource.getTitle(), reconfiguredTaskResource.getTitle());
        assertEquals(taskResource.getDescription(), reconfiguredTaskResource.getDescription());
        assertEquals(taskResource.getCaseName(), reconfiguredTaskResource.getCaseName());
        assertEquals(taskResource.getRegion(), reconfiguredTaskResource.getRegion());
        assertEquals(taskResource.getLocation(), reconfiguredTaskResource.getLocation());
        assertEquals(taskResource.getLocationName(), reconfiguredTaskResource.getLocationName());
        assertEquals(taskResource.getCaseCategory(), reconfiguredTaskResource.getCaseCategory());
        assertEquals(
            taskResource.getWorkTypeResource().getId(),
            reconfiguredTaskResource.getWorkTypeResource().getId()
        );
        assertEquals(taskResource.getRoleCategory(), reconfiguredTaskResource.getRoleCategory());
        assertEquals(taskResource.getPriorityDate(), reconfiguredTaskResource.getPriorityDate());
        assertEquals(taskResource.getMinorPriority(), reconfiguredTaskResource.getMinorPriority());
        assertEquals(taskResource.getMajorPriority(), reconfiguredTaskResource.getMajorPriority());
        assertEquals(taskResource.getNextHearingId(), reconfiguredTaskResource.getNextHearingId());
        assertEquals(taskResource.getNextHearingDate(), reconfiguredTaskResource.getNextHearingDate());
        assertEquals(taskResource.getDueDateTime(), reconfiguredTaskResource.getDueDateTime());
    }

    @Test
    void cannot_reconfigure_a_task_with_data_from_configuration_DMN_when_can_reconfigure_is_null() {

        TaskResource taskResource = createTaskResource();

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponseWithNullReconfigure(),
            permissionsResponse()
        );

        TaskResource reconfiguredTaskResource = cftTaskMapper
            .reconfigureTaskResourceFromDmnResults(taskResource, results);
        assertEquals(taskResource.getTitle(), reconfiguredTaskResource.getTitle());
        assertEquals(taskResource.getDescription(), reconfiguredTaskResource.getDescription());
        assertEquals(taskResource.getCaseName(), reconfiguredTaskResource.getCaseName());
        assertEquals(taskResource.getRegion(), reconfiguredTaskResource.getRegion());
        assertEquals(taskResource.getLocation(), reconfiguredTaskResource.getLocation());
        assertEquals(taskResource.getLocationName(), reconfiguredTaskResource.getLocationName());
        assertEquals(taskResource.getCaseCategory(), reconfiguredTaskResource.getCaseCategory());
        assertEquals(
            taskResource.getWorkTypeResource().getId(),
            reconfiguredTaskResource.getWorkTypeResource().getId()
        );
        assertEquals(taskResource.getRoleCategory(), reconfiguredTaskResource.getRoleCategory());
        assertEquals(taskResource.getPriorityDate(), reconfiguredTaskResource.getPriorityDate());
        assertEquals(taskResource.getMinorPriority(), reconfiguredTaskResource.getMinorPriority());
        assertEquals(taskResource.getMajorPriority(), reconfiguredTaskResource.getMajorPriority());
        assertEquals(taskResource.getNextHearingId(), reconfiguredTaskResource.getNextHearingId());
        assertEquals(taskResource.getNextHearingDate(), reconfiguredTaskResource.getNextHearingDate());
        assertEquals(taskResource.getDueDateTime(), reconfiguredTaskResource.getDueDateTime());
    }

    @Test
    void reconfigure_config_attributes_dmn_fields() {
        TaskResource taskResource = createTaskResource();

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "additionalProperties",
                                               writeValueAsString(Map.of("roleAssignmentId", "1234567890")), true
        );
        assertEquals(taskResource.getAdditionalProperties(), Map.of("roleAssignmentId", "1234567890"));

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "priorityDate",
                                               "2021-05-09T20:15", true
        );
        assertEquals(
            taskResource.getPriorityDate(),
            LocalDateTime.of(2021, 5, 9, 20, 15, 0, 0)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "nextHearingDate",
                                               "2021-05-09T20:15", true
        );
        assertEquals(
            taskResource.getNextHearingDate(),
            LocalDateTime.of(2021, 5, 9, 20, 15, 0, 0)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
        );

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "minorPriority",
                                               1, true
        );
        cftTaskMapper.reconfigureTaskAttribute(taskResource, "majorPriority",
                                               1, true
        );
        assertEquals(1, taskResource.getMinorPriority());
        assertEquals(1, taskResource.getMajorPriority());

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "nextHearingId",
                                               null, true
        );
        assertEquals("nextHearingId", taskResource.getNextHearingId());

        cftTaskMapper.reconfigureTaskAttribute(taskResource, "nextHearingId",
                                               "", true
        );
        assertEquals("nextHearingId", taskResource.getNextHearingId());
    }

    @Test
    void should_map_task_role_permissions_for_granular_permissions() {
        TaskRoleResource roleResource = new TaskRoleResource(
            "ctsc",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{"SPECIFIC", "STANDARD"},
            0,
            false,
            "CTSC",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );

        final TaskRolePermissions taskRolePermissions = cftTaskMapper.mapToTaskRolePermissions(roleResource, true);

        assertEquals("ctsc", taskRolePermissions.getRoleName());
        assertEquals("CTSC", taskRolePermissions.getRoleCategory());
        assertNotNull(taskRolePermissions.getPermissions());
        assertFalse(taskRolePermissions.getPermissions().isEmpty());
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.READ));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.MANAGE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.EXECUTE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.REFER));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CLAIM));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.ASSIGN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_ASSIGN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.COMPLETE_OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.CANCEL_OWN));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.UNASSIGN_CLAIM));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM));
        assertTrue(taskRolePermissions.getPermissions().contains(PermissionTypes.UNCLAIM_ASSIGN));

        assertFalse(taskRolePermissions.getAuthorisations().isEmpty());
        assertTrue(taskRolePermissions.getAuthorisations().contains("SPECIFIC"));
        assertTrue(taskRolePermissions.getAuthorisations().contains("STANDARD"));
    }

    private TaskResource createTaskResource() {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            null,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            emptySet(),
            "caseCategory",
            null,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

    private List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse(boolean canReconfigure) {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                                                   booleanValue(false)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("priorityDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("autoAssigned"), stringValue("true"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                                                   booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("dueDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            )
        );
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponseWithNullReconfigure() {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                                                   booleanValue(false)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("priorityDate"),
                stringValue("2021-05-09T20:15:45"),
                null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                                                   null
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15:45"),
                null
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("dueDate"),
                stringValue("2021-05-09T20:15"),
                null
            )
        );
    }

    private TaskResource createTaskResourceWithRoleResource(TaskRoleResource roleResource) {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            null,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(roleResource),
            "caseCategory",
            EXPECTED_ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

    private Map<String, Object> getDefaultAttributes(String createdDate, String dueDate, String priorityDate) {
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put(CamundaVariableDefinition.ASSIGNEE.value(), "someAssignee");
        attributes.put(CamundaVariableDefinition.AUTO_ASSIGNED.value(), false);
        attributes.put(CamundaVariableDefinition.CASE_CATEGORY.value(), "someCaseCategory");
        attributes.put(CamundaVariableDefinition.CASE_ID.value(), "00000");
        attributes.put(CamundaVariableDefinition.CASE_NAME.value(), "someCaseName");
        attributes.put(CamundaVariableDefinition.CASE_TYPE_ID.value(), "someCaseType");
        if (createdDate != null) {
            attributes.put(CamundaVariableDefinition.CREATED
                               .value(), createdDate);
        }
        if (dueDate != null) {
            attributes.put(CamundaVariableDefinition.DUE_DATE.value(), dueDate);
        }
        if (priorityDate != null) {
            attributes.put(CamundaVariableDefinition.PRIORITY_DATE.value(), priorityDate);
        }
        attributes.put(CamundaVariableDefinition.DESCRIPTION.value(), "someCamundaTaskDescription");
        attributes.put(CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL");
        attributes.put(CamundaVariableDefinition.HAS_WARNINGS.value(), true);
        attributes.put(CamundaVariableDefinition.JURISDICTION.value(), "someJurisdiction");
        attributes.put(CamundaVariableDefinition.LOCATION.value(), "someStaffLocationId");
        attributes.put(CamundaVariableDefinition.LOCATION_NAME.value(), "someStaffLocationName");
        attributes.put(CamundaVariableDefinition.TASK_NAME.value(), "someCamundaTaskName");
        attributes.put(CamundaVariableDefinition.REGION.value(), "someRegion");
        attributes.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "PUBLIC");
        attributes.put(CamundaVariableDefinition.TASK_STATE.value(), CFTTaskState.UNCONFIGURED);
        attributes.put(CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF");
        attributes.put(CamundaVariableDefinition.TITLE.value(), "someTitle");
        attributes.put(CamundaVariableDefinition.TASK_TYPE.value(), "someTaskType");
        attributes.put(CamundaVariableDefinition.ADDITIONAL_PROPERTIES.value(), EXPECTED_ADDITIONAL_PROPERTIES);
        attributes.put(CamundaVariableDefinition.NEXT_HEARING_ID.value(), "nextHearingId");
        attributes.put(
            CamundaVariableDefinition.NEXT_HEARING_DATE.value(),
            CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())
        );
        return attributes;
    }

    private Map<String, Object> getDefaultAttributesWithoutDueDate() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CamundaVariableDefinition.ASSIGNEE.value(), "someAssignee");
        attributes.put(CamundaVariableDefinition.AUTO_ASSIGNED.value(), false);
        attributes.put(CamundaVariableDefinition.CASE_CATEGORY.value(), "someCaseCategory");
        attributes.put(CamundaVariableDefinition.CASE_ID.value(), "00000");
        attributes.put(CamundaVariableDefinition.CASE_NAME.value(), "someCaseName");
        attributes.put(CamundaVariableDefinition.CASE_TYPE_ID.value(), "someCaseType");
        attributes.put(CamundaVariableDefinition.DESCRIPTION.value(), "someCamundaTaskDescription");
        attributes.put(CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL");
        attributes.put(CamundaVariableDefinition.HAS_WARNINGS.value(), false);
        attributes.put(CamundaVariableDefinition.JURISDICTION.value(), "someJurisdiction");
        attributes.put(CamundaVariableDefinition.LOCATION.value(), "someStaffLocationId");
        attributes.put(CamundaVariableDefinition.LOCATION_NAME.value(), "someStaffLocationName");
        attributes.put(CamundaVariableDefinition.TASK_NAME.value(), "someCamundaTaskName");
        attributes.put(CamundaVariableDefinition.REGION.value(), "someRegion");
        attributes.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "PUBLIC");
        attributes.put(CamundaVariableDefinition.TASK_STATE.value(), CFTTaskState.UNCONFIGURED);
        attributes.put(CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF");
        attributes.put(CamundaVariableDefinition.TITLE.value(), "someTitle");
        attributes.put(CamundaVariableDefinition.TASK_TYPE.value(), "someTaskType");
        attributes.put(CamundaVariableDefinition.ADDITIONAL_PROPERTIES.value(), EXPECTED_ADDITIONAL_PROPERTIES);
        attributes.put(CamundaVariableDefinition.NEXT_HEARING_ID.value(), "nextHearingId");
        attributes.put(
            CamundaVariableDefinition.NEXT_HEARING_DATE.value(),
            CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())
        );
        return attributes;
    }

    private Map<String, Object> getDefaultAttributesWithWorkType(String createdDate, String dueDate) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CamundaVariableDefinition.ASSIGNEE.value(), "someAssignee");
        attributes.put(CamundaVariableDefinition.AUTO_ASSIGNED.value(), false);
        attributes.put(CamundaVariableDefinition.CASE_CATEGORY.value(), "someCaseCategory");
        attributes.put(CamundaVariableDefinition.CASE_ID.value(), "00000");
        attributes.put(CamundaVariableDefinition.CASE_NAME.value(), "someCaseName");
        attributes.put(CamundaVariableDefinition.CASE_TYPE_ID.value(), "someCaseType");
        attributes.put(CamundaVariableDefinition.CREATED.value(), createdDate);
        attributes.put(CamundaVariableDefinition.DUE_DATE.value(), dueDate);
        attributes.put(CamundaVariableDefinition.DESCRIPTION.value(), "someCamundaTaskDescription");
        attributes.put(CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL");
        attributes.put(CamundaVariableDefinition.HAS_WARNINGS.value(), false);
        attributes.put(CamundaVariableDefinition.JURISDICTION.value(), "someJurisdiction");
        attributes.put(CamundaVariableDefinition.LOCATION.value(), "someStaffLocationId");
        attributes.put(CamundaVariableDefinition.LOCATION_NAME.value(), "someStaffLocationName");
        attributes.put(CamundaVariableDefinition.TASK_NAME.value(), "someCamundaTaskName");
        attributes.put(CamundaVariableDefinition.REGION.value(), "someRegion");
        attributes.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "PUBLIC");
        attributes.put(CamundaVariableDefinition.TASK_STATE.value(), CFTTaskState.UNCONFIGURED);
        attributes.put(CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF");
        attributes.put(CamundaVariableDefinition.TITLE.value(), "someTitle");
        attributes.put(CamundaVariableDefinition.TASK_TYPE.value(), "someTaskType");
        attributes.put(CamundaVariableDefinition.WORK_TYPE.value(), "someWorkType");
        attributes.put(CamundaVariableDefinition.ADDITIONAL_PROPERTIES.value(), EXPECTED_ADDITIONAL_PROPERTIES);
        attributes.put(CamundaVariableDefinition.NEXT_HEARING_ID.value(), "nextHearingId");
        attributes.put(
            CamundaVariableDefinition.NEXT_HEARING_DATE.value(),
            CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())
        );
        return attributes;
    }

    private Map<String, Object> getDefaultAttributesWithoutWithWorkType(String createdDate, String dueDate) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CamundaVariableDefinition.ASSIGNEE.value(), "someAssignee");
        attributes.put(CamundaVariableDefinition.AUTO_ASSIGNED.value(), false);
        attributes.put(CamundaVariableDefinition.CASE_CATEGORY.value(), "someCaseCategory");
        attributes.put(CamundaVariableDefinition.CASE_ID.value(), "00000");
        attributes.put(CamundaVariableDefinition.CASE_NAME.value(), "someCaseName");
        attributes.put(CamundaVariableDefinition.CASE_TYPE_ID.value(), "someCaseType");
        attributes.put(CamundaVariableDefinition.CREATED
                           .value(), createdDate);
        attributes.put(CamundaVariableDefinition.DUE_DATE.value(), dueDate);
        attributes.put(CamundaVariableDefinition.DESCRIPTION.value(), "someCamundaTaskDescription");
        attributes.put(CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL");
        attributes.put(CamundaVariableDefinition.HAS_WARNINGS.value(), false);
        attributes.put(CamundaVariableDefinition.JURISDICTION.value(), "someJurisdiction");
        attributes.put(CamundaVariableDefinition.LOCATION.value(), "someStaffLocationId");
        attributes.put(CamundaVariableDefinition.LOCATION_NAME.value(), "someStaffLocationName");
        attributes.put(CamundaVariableDefinition.TASK_NAME.value(), "someCamundaTaskName");
        attributes.put(CamundaVariableDefinition.REGION.value(), "someRegion");
        attributes.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "PUBLIC");
        attributes.put(CamundaVariableDefinition.TASK_STATE.value(), CFTTaskState.UNCONFIGURED);
        attributes.put(CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF");
        attributes.put(CamundaVariableDefinition.TITLE.value(), "someTitle");
        attributes.put(CamundaVariableDefinition.TASK_TYPE.value(), "someTaskType");
        attributes.put(CamundaVariableDefinition.ADDITIONAL_PROPERTIES.value(), EXPECTED_ADDITIONAL_PROPERTIES);
        attributes.put(CamundaVariableDefinition.NEXT_HEARING_ID.value(), "nextHearingId");
        attributes.put(
            CamundaVariableDefinition.NEXT_HEARING_DATE.value(),
            CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())
        );
        return attributes;
    }

    private Map<String, Object> getDefaultAttributesWithWarnings(String createdDate, String dueDate) {
        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
            + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CamundaVariableDefinition.ASSIGNEE.value(), "someAssignee");
        attributes.put(CamundaVariableDefinition.AUTO_ASSIGNED.value(), false);
        attributes.put(CamundaVariableDefinition.CASE_CATEGORY.value(), "someCaseCategory");
        attributes.put(CamundaVariableDefinition.CASE_ID.value(), "00000");
        attributes.put(CamundaVariableDefinition.CASE_NAME.value(), "someCaseName");
        attributes.put(CamundaVariableDefinition.CASE_TYPE_ID.value(), "someCaseType");
        attributes.put(CamundaVariableDefinition.CREATED
                           .value(), createdDate);
        attributes.put(CamundaVariableDefinition.DUE_DATE.value(), dueDate);
        attributes.put(CamundaVariableDefinition.DESCRIPTION.value(), "someCamundaTaskDescription");
        attributes.put(CamundaVariableDefinition.EXECUTION_TYPE.value(), "MANUAL");
        attributes.put(CamundaVariableDefinition.HAS_WARNINGS.value(), true);
        attributes.put(CamundaVariableDefinition.JURISDICTION.value(), "someJurisdiction");
        attributes.put(CamundaVariableDefinition.LOCATION.value(), "someStaffLocationId");
        attributes.put(CamundaVariableDefinition.LOCATION_NAME.value(), "someStaffLocationName");
        attributes.put(CamundaVariableDefinition.TASK_NAME.value(), "someCamundaTaskName");
        attributes.put(CamundaVariableDefinition.REGION.value(), "someRegion");
        attributes.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), "PUBLIC");
        attributes.put(CamundaVariableDefinition.TASK_STATE.value(), CFTTaskState.UNCONFIGURED);
        attributes.put(CamundaVariableDefinition.TASK_SYSTEM.value(), "SELF");
        attributes.put(CamundaVariableDefinition.TITLE.value(), "someTitle");
        attributes.put(CamundaVariableDefinition.TASK_TYPE.value(), "someTaskType");
        attributes.put(WARNING_LIST.value(), values);
        attributes.put(CamundaVariableDefinition.ADDITIONAL_PROPERTIES.value(), EXPECTED_ADDITIONAL_PROPERTIES);
        attributes.put(CamundaVariableDefinition.NEXT_HEARING_ID.value(), "nextHearingId");
        attributes.put(
            CamundaVariableDefinition.NEXT_HEARING_DATE.value(),
            CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())
        );
        return attributes;
    }

    private String writeValueAsString(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
