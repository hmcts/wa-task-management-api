package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_MANAGEMENT_CATEGORY;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;

@ExtendWith(MockitoExtension.class)
class CFTTaskMapperTest {

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
        List<TaskAttribute> attributes = new ArrayList<>();
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        attributes.add(new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"));
        attributes.add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));
        attributes.add(null);
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

        List<TaskAttribute> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate);

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
        assertNull(taskResource.getMajorPriority());
        assertNull(taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getWorkTypeResource());
        assertNull(taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
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
    }

    @Test
    void should_map_task_attributes_to_cft_task_with_warnings() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);


        List<TaskAttribute> attributes = getDefaultAttributesWithWarnings(formattedCreatedDate, formattedDueDate);

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
        assertNull(taskResource.getMajorPriority());
        assertNull(taskResource.getMinorPriority());
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
    }

    @Test
    void should_map_cftTask_to_task_with_warnings() {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> attributes = getDefaultAttributesWithWarnings(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Task task = cftTaskMapper.mapToTask(taskResource);

        assertNotNull(task);
        assertTrue(task.getWarnings());
        assertNotNull(task.getWarningList());
        assertNotNull(task.getWarningList().getValues());
        assertEquals(2, task.getWarningList().getValues().size());

    }

    @Test
    void should_map_cft_task_to_task_with_create_date_is_null() {

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        List<TaskAttribute> attributes = getDefaultAttributesWithWarnings(null, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        assertNull(taskResource.getCreated());

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

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues));


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
        assertEquals(emptySet(), taskResource.getTaskRoleResources());
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

        List<PermissionsDmnEvaluationResponse> permissionsDmnEvaluationResponses =
            asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    null,
                    null,
                    stringValue("LEGAL_OPERATIONS")
                )
            );

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, emptyList(), permissionsDmnEvaluationResponses));


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
        List<TaskRoleResource> roleResourcesList = new ArrayList<>();
        roleResourcesList.addAll(taskResource.getTaskRoleResources());

        assertEquals("senior-tribunal-caseworker", roleResourcesList.get(0).getRoleName());
        assertEquals(true, roleResourcesList.get(0).getRead());
        assertEquals(true, roleResourcesList.get(0).getOwn());
        assertEquals(false, roleResourcesList.get(0).getExecute());
        assertEquals(true, roleResourcesList.get(0).getManage());
        assertEquals(true, roleResourcesList.get(0).getCancel());
        assertEquals(true, roleResourcesList.get(0).getRefer());
        assertArrayEquals(new String[]{}, roleResourcesList.get(0).getAuthorizations());
        assertEquals("tribunal-caseworker", roleResourcesList.get(1).getRoleName());
        assertEquals(true, roleResourcesList.get(1).getRead());
        assertEquals(true, roleResourcesList.get(1).getOwn());
        assertEquals(false, roleResourcesList.get(1).getExecute());
        assertEquals(true, roleResourcesList.get(1).getManage());
        assertEquals(true, roleResourcesList.get(1).getCancel());
        assertEquals(true, roleResourcesList.get(1).getRefer());
        assertArrayEquals(new String[]{"IA", "WA"}, roleResourcesList.get(1).getAuthorizations());
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

        List<PermissionsDmnEvaluationResponse> permissionsDmnEvaluationResponses =
            asList(
                new PermissionsDmnEvaluationResponse(
                    stringValue("tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    stringValue("IA,WA"),
                    integerValue(2),
                    booleanValue(true),
                    stringValue("LEGAL_OPERATIONS")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("senior-tribunal-caseworker"),
                    stringValue("Read,Refer,Own,Manage,Cancel"),
                    null,
                    integerValue(1),
                    booleanValue(false),
                    stringValue("LEGAL_OPERATIONS")
                ),
                new PermissionsDmnEvaluationResponse(
                    stringValue("some-caseworker"),
                    stringValue("Read"),
                    null,
                    integerValue(null),
                    booleanValue(null),
                    stringValue(null)
                )
            );

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, emptyList(), permissionsDmnEvaluationResponses));


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

    }

    @Test
    void should_throw_exception_when_execution_type_enum_is_not_mapped() {

        List<TaskAttribute> attributes = singletonList(
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "someExecutionType")
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ExecutionTypeName value: 'someExecutionType' could not be mapped to ExecutionType enum")
            .hasNoCause();
    }

    @Test
    void should_throw_exception_when_no_due_date() {

        List<TaskAttribute> attributes = asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someCaseId")
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("TASK_DUE_DATE must not be null")
            .hasNoCause();
    }

    @Test
    void should_throw_exception_when_task_system_enum_is_not_mapped() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> attributes = asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "someTaskSystem"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)

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

        List<TaskAttribute> attributes = asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "someInvalidEnumValue"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        );

        assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(
                "Cannot deserialize value of type "
                + "`uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification` "
                + "from String \"someInvalidEnumValue\": not one of the values accepted for Enum class: "
                + "[PUBLIC, RESTRICTED, PRIVATE]")
            .hasCauseInstanceOf(InvalidFormatException.class);

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
            new TaskConfigurationResults(mappedValues));

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

        List<TaskAttribute> attributes = getDefaultAttributesWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Task task = cftTaskMapper.mapToTask(taskResource);

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

        List<TaskAttribute> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Task task = cftTaskMapper.mapToTask(taskResource);

        assertNull(task.getWorkTypeId());

        assertNull(taskResource.getWorkTypeResource());
    }

    @Test
    void should_map_task_resource_to_task_when_work_type_not_exists() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> attributes =
            getDefaultAttributesWithoutWithWorkType(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        taskResource.setWorkTypeResource(null);

        Task task = cftTaskMapper.mapToTask(taskResource);

        assertNull(task.getWorkTypeId());

        AssertionsForClassTypes.assertThatThrownBy(() -> taskResource.getWorkTypeResource().getId())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_exception_when_map_task_resource_to_task_and_due_date_is_null() {
        List<TaskAttribute> attributes = getDefaultAttributesWithoutDueDate();

        AssertionsForClassTypes.assertThatThrownBy(() -> cftTaskMapper.mapToTaskResource(taskId, attributes))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("TASK_DUE_DATE must not be null");
    }

    @Test
    void should_throw_exception_when_map_to_task_and_create_date_is_null() {
        String formattedCreatedDate = null;
        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        assertNull(taskResource.getCreated());
        assertNotNull(taskResource.getDueDateTime());

        assertThatThrownBy(() -> cftTaskMapper.mapToTask(taskResource))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_exception_when_map_to_task_and_due_date_is_null() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        List<TaskAttribute> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        assertNotNull(taskResource.getDueDateTime());

        //given
        taskResource.setDueDateTime(null);
        assertThatThrownBy(() -> cftTaskMapper.mapToTask(taskResource))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_map_task_resource_to_task_with_permissions_empty() {

        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseofficer",
            false,
            false,
            false,
            false,
            false,
            false,
            new String[]{"SPECIFIC", "BASIC"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
        TaskResource taskResource = createTaskResourceWithRoleResource(roleResource);
        Task task = cftTaskMapper.mapToTask(taskResource);

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
        Assertions.assertTrue(task.getPermissions().getValues().isEmpty());
    }

    @Test
    void should_map_task_resource_to_task_with_permissions() {

        TaskRoleResource roleResource = new TaskRoleResource(
            "tribunal-caseofficer",
            true,
            true,
            true,
            true,
            true,
            true,
            new String[]{"SPECIFIC", "BASIC"},
            0,
            false,
            "JUDICIAL",
            "taskId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
        TaskResource taskResource = createTaskResourceWithRoleResource(roleResource);
        Task task = cftTaskMapper.mapToTask(taskResource);

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
        Assertions.assertFalse(task.getPermissions().getValues().isEmpty());
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.READ));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.OWN));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.MANAGE));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.EXECUTE));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.CANCEL));
        assertTrue(task.getPermissions().getValues().contains(PermissionTypes.REFER));
    }

    @Test
    void should_map_task_resource_to_task() {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);


        List<TaskAttribute> attributes = getDefaultAttributes(formattedCreatedDate, formattedDueDate);

        TaskResource taskResource = cftTaskMapper.mapToTaskResource(taskId, attributes);
        Task task = cftTaskMapper.mapToTask(taskResource);

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
        assertEquals(false, task.getWarnings());
        assertEquals(emptyList(), task.getWarningList().getValues());
        assertEquals("someCaseCategory", task.getCaseManagementCategory());
        assertNotNull(task.getDueDate());
        assertNotNull(task.getCreatedDate());
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
            "caseCategory"
        );
    }

    private List<TaskAttribute> getDefaultAttributes(String createdDate, String dueDate) {
        return asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TaskAttributeDefinition.TASK_AUTO_ASSIGNED, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_CATEGORY, "someCaseCategory"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_ID, "00000"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_NAME, "someCaseName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_TYPE_ID, "someCaseType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CREATED, createdDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DUE_DATE, dueDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DESCRIPTION, "someCamundaTaskDescription"),
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_HAS_WARNINGS, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_JURISDICTION, "someJurisdiction"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION, "someStaffLocationId"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION_NAME, "someStaffLocationName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, "someCamundaTaskName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION, "someRegion"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "PUBLIC"),
            new TaskAttribute(TaskAttributeDefinition.TASK_STATE, CFTTaskState.UNCONFIGURED),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TITLE, "someTitle"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "someTaskType"),
            //Unmapped
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_BUSINESS_CONTEXT, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MAJOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MINOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLES, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLE_CATEGORY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION_NAME, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_TERMINATION_REASON, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_WORK_TYPE, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_NOTES, null)
        );
    }

    private List<TaskAttribute> getDefaultAttributesWithoutDueDate() {
        return asList(new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TaskAttributeDefinition.TASK_AUTO_ASSIGNED, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_CATEGORY, "someCaseCategory"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_ID, "00000"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_NAME, "someCaseName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_TYPE_ID, "someCaseType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CREATED, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_DUE_DATE, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_DESCRIPTION, "someCamundaTaskDescription"),
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_HAS_WARNINGS, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_JURISDICTION, "someJurisdiction"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION, "someStaffLocationId"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION_NAME, "someStaffLocationName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, "someCamundaTaskName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION, "someRegion"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "PUBLIC"),
            new TaskAttribute(TaskAttributeDefinition.TASK_STATE, CFTTaskState.UNCONFIGURED),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TITLE, "someTitle"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "someTaskType"),
            //Unmapped
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_BUSINESS_CONTEXT, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MAJOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MINOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLES, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLE_CATEGORY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION_NAME, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_TERMINATION_REASON, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_WORK_TYPE, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_NOTES, null)
        );
    }

    private List<TaskAttribute> getDefaultAttributesWithWorkType(String createdDate, String dueDate) {
        return asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TaskAttributeDefinition.TASK_AUTO_ASSIGNED, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_CATEGORY, "someCaseCategory"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_ID, "00000"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_NAME, "someCaseName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_TYPE_ID, "someCaseType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CREATED, createdDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DUE_DATE, dueDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DESCRIPTION, "someCamundaTaskDescription"),
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_HAS_WARNINGS, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_JURISDICTION, "someJurisdiction"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION, "someStaffLocationId"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION_NAME, "someStaffLocationName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, "someCamundaTaskName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION, "someRegion"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "PUBLIC"),
            new TaskAttribute(TaskAttributeDefinition.TASK_STATE, CFTTaskState.UNCONFIGURED),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TITLE, "someTitle"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "someTaskType"),
            //Unmapped
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_BUSINESS_CONTEXT, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MAJOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MINOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLES, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLE_CATEGORY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION_NAME, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_TERMINATION_REASON, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_WORK_TYPE, "someWorkType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NOTES, null)
        );
    }

    private List<TaskAttribute> getDefaultAttributesWithoutWithWorkType(String createdDate, String dueDate) {
        return asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TaskAttributeDefinition.TASK_AUTO_ASSIGNED, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_CATEGORY, "someCaseCategory"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_ID, "00000"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_NAME, "someCaseName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_TYPE_ID, "someCaseType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CREATED, createdDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DUE_DATE, dueDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DESCRIPTION, "someCamundaTaskDescription"),
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_HAS_WARNINGS, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_JURISDICTION, "someJurisdiction"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION, "someStaffLocationId"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION_NAME, "someStaffLocationName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, "someCamundaTaskName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION, "someRegion"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "PUBLIC"),
            new TaskAttribute(TaskAttributeDefinition.TASK_STATE, CFTTaskState.UNCONFIGURED),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TITLE, "someTitle"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "someTaskType"),
            //Unmapped
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_BUSINESS_CONTEXT, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MAJOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MINOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLES, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLE_CATEGORY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION_NAME, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_TERMINATION_REASON, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_NOTES, null)
        );
    }

    private List<TaskAttribute> getDefaultAttributesWithWarnings(String createdDate, String dueDate) {
        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                        + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";
        return asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TaskAttributeDefinition.TASK_AUTO_ASSIGNED, false),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_CATEGORY, "someCaseCategory"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_ID, "00000"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_NAME, "someCaseName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CASE_TYPE_ID, "someCaseType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_CREATED, createdDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DUE_DATE, dueDate),
            new TaskAttribute(TaskAttributeDefinition.TASK_DESCRIPTION, "someCamundaTaskDescription"),
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_HAS_WARNINGS, true),
            new TaskAttribute(TaskAttributeDefinition.TASK_JURISDICTION, "someJurisdiction"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION, "someStaffLocationId"),
            new TaskAttribute(TaskAttributeDefinition.TASK_LOCATION_NAME, "someStaffLocationName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_NAME, "someCamundaTaskName"),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION, "someRegion"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "PUBLIC"),
            new TaskAttribute(TaskAttributeDefinition.TASK_STATE, CFTTaskState.UNCONFIGURED),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TITLE, "someTitle"),
            new TaskAttribute(TaskAttributeDefinition.TASK_TYPE, "someTaskType"),
            new TaskAttribute(TaskAttributeDefinition.TASK_WARNINGS, values),
            //Unmapped
            new TaskAttribute(TaskAttributeDefinition.TASK_ASSIGNMENT_EXPIRY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_BUSINESS_CONTEXT, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MAJOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_MINOR_PRIORITY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLES, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_ROLE_CATEGORY, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_REGION_NAME, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_TERMINATION_REASON, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_WORK_TYPE, null),
            new TaskAttribute(TaskAttributeDefinition.TASK_NOTES, null)
        );


    }
}
