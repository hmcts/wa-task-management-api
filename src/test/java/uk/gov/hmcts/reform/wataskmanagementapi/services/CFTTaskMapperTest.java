package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
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
        assertEquals(null, taskResource.getNotes());
        assertEquals(null, taskResource.getMajorPriority());
        assertEquals(null, taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertEquals(null, taskResource.getWorkTypeResource());
        assertEquals(null, taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertEquals(null, taskResource.getAssignmentExpiry());
        assertEquals("00000", taskResource.getCaseId());
        assertEquals("someCaseType", taskResource.getCaseTypeId());
        assertEquals("someCaseName", taskResource.getCaseName());
        assertEquals("someJurisdiction", taskResource.getJurisdiction());
        assertEquals("someRegion", taskResource.getRegion());
        assertEquals(null, taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals(null, taskResource.getBusinessContext());
        assertEquals(null, taskResource.getTerminationReason());
        assertEquals(
            OffsetDateTime.parse(formattedCreatedDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getCreated()
        );
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertEquals(null, taskResource.getTaskRoleResources());
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
        assertEquals(null, taskResource.getMajorPriority());
        assertEquals(null, taskResource.getMinorPriority());
        assertEquals("someAssignee", taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertEquals(null, taskResource.getWorkTypeResource());
        assertEquals(null, taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertEquals(null, taskResource.getAssignmentExpiry());
        assertEquals("00000", taskResource.getCaseId());
        assertEquals("someCaseType", taskResource.getCaseTypeId());
        assertEquals("someCaseName", taskResource.getCaseName());
        assertEquals("someJurisdiction", taskResource.getJurisdiction());
        assertEquals("someRegion", taskResource.getRegion());
        assertEquals(null, taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals(null, taskResource.getBusinessContext());
        assertEquals(null, taskResource.getTerminationReason());
        assertEquals(
            OffsetDateTime.parse(formattedCreatedDate, CAMUNDA_DATA_TIME_FORMATTER),
            taskResource.getCreated()
        );
        assertEquals(new ExecutionTypeResource(
            ExecutionType.MANUAL,
            ExecutionType.MANUAL.getName(),
            ExecutionType.MANUAL.getDescription()
        ), taskResource.getExecutionTypeCode());
        assertEquals(null, taskResource.getTaskRoleResources());
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
        assertEquals(null, taskResource.getDescription());
        assertEquals(null, taskResource.getNotes());
        assertEquals(null, taskResource.getMajorPriority());
        assertEquals(null, taskResource.getMinorPriority());
        assertEquals(null, taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertEquals(null, taskResource.getWorkTypeResource());
        assertEquals(null, taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertEquals(null, taskResource.getAssignmentExpiry());
        assertEquals("someCaseId", taskResource.getCaseId());
        assertEquals("someCaseTypeId", taskResource.getCaseTypeId());
        assertEquals("Bob Smith", taskResource.getCaseName());
        assertEquals("IA", taskResource.getJurisdiction());
        assertEquals("1", taskResource.getRegion());
        assertEquals(null, taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals("someCaseCategory", taskResource.getCaseCategory());
        assertEquals(null, taskResource.getBusinessContext());
        assertEquals(null, taskResource.getTerminationReason());
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
        assertEquals(null, taskResource.getDescription());
        assertEquals(null, taskResource.getNotes());
        assertEquals(null, taskResource.getMajorPriority());
        assertEquals(null, taskResource.getMinorPriority());
        assertEquals(null, taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertEquals(null, taskResource.getWorkTypeResource());
        assertEquals(null, taskResource.getRoleCategory());
        assertEquals(false, taskResource.getHasWarnings());
        assertEquals(null, taskResource.getAssignmentExpiry());
        assertEquals("someCaseId", taskResource.getCaseId());
        assertEquals("someCaseTypeId", taskResource.getCaseTypeId());
        assertEquals("Bob Smith", taskResource.getCaseName());
        assertEquals("IA", taskResource.getJurisdiction());
        assertEquals("1", taskResource.getRegion());
        assertEquals(null, taskResource.getRegionName());
        assertEquals("someStaffLocationId", taskResource.getLocation());
        assertEquals("someStaffLocationName", taskResource.getLocationName());
        assertEquals("someCaseCategory", taskResource.getCaseCategory());
        assertEquals(null, taskResource.getBusinessContext());
        assertEquals(null, taskResource.getTerminationReason());
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
        assertEquals(emptyList(), roleResourcesList.get(0).getAuthorizations());
        assertEquals("tribunal-caseworker", roleResourcesList.get(1).getRoleName());
        assertEquals(true, roleResourcesList.get(1).getRead());
        assertEquals(true, roleResourcesList.get(1).getOwn());
        assertEquals(false, roleResourcesList.get(1).getExecute());
        assertEquals(true, roleResourcesList.get(1).getManage());
        assertEquals(true, roleResourcesList.get(1).getCancel());
        assertEquals(true, roleResourcesList.get(1).getRefer());
        assertEquals(asList("IA", "WA"), roleResourcesList.get(1).getAuthorizations());
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
    void should_throw_exception_when_task_system_enum_is_not_mapped() {

        List<TaskAttribute> attributes = asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "someTaskSystem")
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

        List<TaskAttribute> attributes = asList(
            new TaskAttribute(TaskAttributeDefinition.TASK_EXECUTION_TYPE_NAME, "MANUAL"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SYSTEM, "SELF"),
            new TaskAttribute(TaskAttributeDefinition.TASK_SECURITY_CLASSIFICATION, "someInvalidEnumValue")

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


    private List<TaskAttribute> getDefaultAttributesWithWarnings(String createdDate, String dueDate) {

        List<TaskAttribute> defaultAttributes = getDefaultAttributes(createdDate, dueDate);
        List<TaskAttribute> attributes = new ArrayList<>(defaultAttributes);
        String values = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                        + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";
        attributes.add(new TaskAttribute(TaskAttributeDefinition.TASK_WARNINGS, values));

        return attributes;
    }


}
