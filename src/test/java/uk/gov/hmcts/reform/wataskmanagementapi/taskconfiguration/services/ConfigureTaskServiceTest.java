package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

@ExtendWith(MockitoExtension.class)
class ConfigureTaskServiceTest {

    private static final String ASSIGNEE = "assignee1";
    private TaskToConfigure task;
    private TaskConfigurationCamundaService camundaService;
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private TaskAutoAssignmentService autoAssignmentService;
    private CFTTaskMapper cftTaskMapper;
    @Spy
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        cftTaskMapper = new CFTTaskMapper(objectMapper);
        camundaService = mock(TaskConfigurationCamundaService.class);
        taskVariableExtractor = mock(TaskConfigurator.class);
        autoAssignmentService = mock(TaskAutoAssignmentService.class);
        configureTaskService = new ConfigureTaskService(
            camundaService,
            singletonList(taskVariableExtractor),
            autoAssignmentService,
            cftTaskMapper
        );

        task = new TaskToConfigure(
            "taskId",
            "taskTypeId",
            "caseId",
            "taskName"
        );
    }

    @Test
    void can_configure_a_task_with_variables() {

        String processInstanceId = "processInstanceId";

        CamundaTask camundaTask = new CamundaTask(
            task.getId(),
            processInstanceId,
            task.getName()
        );
        when(camundaService.getTask(task.getId())).thenReturn(camundaTask);

        HashMap<String, CamundaValue<?>> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaValue<>(task.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaValue<>(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            CamundaValue.stringValue("taskTypeId")
        );

        doReturn(processVariables).when(camundaService).getVariables(task.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        configureTaskService.configureTask(task.getId());

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("key1", CamundaValue.stringValue("value1"));
        modifications.put("key2", CamundaValue.stringValue("value2"));
        modifications.put(TASK_TYPE.value(), CamundaValue.stringValue("taskTypeId"));
        modifications.put(TASK_STATE.value(), CamundaValue.stringValue(CONFIGURED.value()));

        verify(camundaService).addProcessVariables(
            task.getId(),
            modifications
        );

        verify(autoAssignmentService, times(1)).autoAssignTask(
            eq(task),
            eq(mappedValues.get(TASK_STATE.value()).toString())
        );

    }

    @Test
    void can_configure_a_task_with_no_extra_variables() {

        String processInstanceId = "processInstanceId";

        CamundaTask camundaTask = new CamundaTask(
            task.getId(),
            processInstanceId,
            task.getName()
        );
        when(camundaService.getTask(task.getId())).thenReturn(camundaTask);

        HashMap<String, CamundaValue<?>> processVariables = new HashMap<>();
        processVariables.put(
            CASE_ID.value(),
            new CamundaValue<>(task.getCaseId(), "String")
        );
        processVariables.put(
            TASK_STATE.value(),
            new CamundaValue<>(UNCONFIGURED.value(), "String")
        );
        processVariables.put(
            TASK_ID.value(),
            CamundaValue.stringValue("taskTypeId")
        );

        doReturn(processVariables).when(camundaService).getVariables(task.getId());

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());
        mappedValues.put(TASK_TYPE.value(), "taskTypeId");

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues));

        configureTaskService.configureTask(task.getId());

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put(TASK_STATE.value(), CamundaValue.stringValue(CONFIGURED.value()));
        modifications.put(TASK_TYPE.value(), CamundaValue.stringValue("taskTypeId"));

        verify(camundaService).addProcessVariables(
            task.getId(),
            modifications
        );
    }

    @Test
    void try_to_configure_a_task_that_does_not_exist() {
        String taskIdThatDoesNotExist = "doesNotExist";
        when(camundaService.getTask(taskIdThatDoesNotExist))
            .thenThrow(new ResourceNotFoundException("exception message", new Exception()));

        assertThatThrownBy(() -> configureTaskService.configureTask(taskIdThatDoesNotExist))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("exception message");
    }

    @Test
    void should_get_configuration_with_assignee() {

        final AutoAssignmentResult autoAssignmentResult =
            new AutoAssignmentResult(
                TaskState.ASSIGNED.value(),
                "assignee1"
            );

        when(autoAssignmentService.getAutoAssignmentVariables(task))
            .thenReturn(autoAssignmentResult);

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues));


        final ConfigureTaskResponse configureTaskResponse =
            configureTaskService.getConfiguration(task);

        assertNotNull(configureTaskResponse);
        assertEquals(configureTaskResponse.getTaskId(), task.getId());
        assertEquals(configureTaskResponse.getCaseId(), task.getCaseId());
        assertEquals(ASSIGNEE, configureTaskResponse.getAssignee());
    }

    @Test
    void should_return_autoAssigned_false_when_assignee_null() {

        final AutoAssignmentResult autoAssignmentResult =
            new AutoAssignmentResult(
                TaskState.ASSIGNED.value(),
                null
            );

        when(autoAssignmentService.getAutoAssignmentVariables(task))
            .thenReturn(autoAssignmentResult);

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put("autoAssigned", "false");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues));


        final ConfigureTaskResponse configureTaskResponse =
            configureTaskService.getConfiguration(task);

        assertNotNull(configureTaskResponse);
        assertEquals(configureTaskResponse.getTaskId(), task.getId());
        assertEquals(configureTaskResponse.getCaseId(), task.getCaseId());
        assertNull(configureTaskResponse.getAssignee());
        assertThat(configureTaskResponse.getConfigurationVariables().get(AUTO_ASSIGNED.value())).isEqualTo("false");
    }

    @Test
    void should_get_configuration_with_no_assignee() {

        final AutoAssignmentResult result = new AutoAssignmentResult(UNASSIGNED.value(), null);

        when(autoAssignmentService.getAutoAssignmentVariables(task))
            .thenReturn(result);

        HashMap<String, Object> mappedValues = new HashMap<>();
        mappedValues.put("key1", "value1");
        mappedValues.put("key2", "value2");
        mappedValues.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues));


        final ConfigureTaskResponse configureTaskResponse =
            configureTaskService.getConfiguration(task);

        assertNotNull(configureTaskResponse);
        assertEquals(configureTaskResponse.getTaskId(), task.getId());
        assertEquals(configureTaskResponse.getCaseId(), task.getCaseId());
    }

    @Test
    void should_configure_a_CFT_task_with_variables() {
        TaskResource skeletonTask = new TaskResource(
            task.getId(),
            "someCamundaTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
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

        HashMap<String, Object> mappedConfigurationVariables = new HashMap<>();
        mappedConfigurationVariables.put("key1", "value1");
        mappedConfigurationVariables.put("key2", "value2");
        mappedConfigurationVariables.put(TASK_TYPE.value(), "taskTypeId");
        mappedConfigurationVariables.put(TASK_STATE.value(), CONFIGURED.value());

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedConfigurationVariables));

        taskResource = configureTaskService.configureCFTTask(taskResource, task);

        assertNotNull(taskResource);
        assertEquals(task.getId(), taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("taskTypeId", taskResource.getTaskType());
        assertEquals(CFTTaskState.UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertNull(taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertNull(taskResource.getMajorPriority());
        assertNull(taskResource.getMinorPriority());
        assertNull(taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
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
    void should_configure_a_cft_task_with_permission_dmn_and_configuration_dmn() {
        TaskResource skeletonTask = new TaskResource(
            task.getId(),
            "someCamundaTaskName",
            "someTaskType",
            CFTTaskState.UNCONFIGURED,
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

        List<ConfigurationDmnEvaluationResponse> configurationDmnEvaluationResponses = asList(
            new ConfigurationDmnEvaluationResponse(stringValue("name1"), stringValue("value1")),
            new ConfigurationDmnEvaluationResponse(stringValue("name2"), stringValue("value2"))
        );

        when(taskVariableExtractor.getConfigurationVariables(task))
            .thenReturn(new TaskConfigurationResults(mappedValues, configurationDmnEvaluationResponses, permissionsDmnEvaluationResponses));

        TaskResource taskResource = cftTaskMapper.mapConfigurationAttributes(
            skeletonTask,
            new TaskConfigurationResults(mappedValues, configurationDmnEvaluationResponses, permissionsDmnEvaluationResponses));

        taskResource = configureTaskService.configureCFTTask(taskResource, task);

        assertEquals("taskId", taskResource.getTaskId());
        assertEquals("someCamundaTaskName", taskResource.getTaskName());
        assertEquals("someTaskType", taskResource.getTaskType());
        assertEquals(CFTTaskState.UNCONFIGURED, taskResource.getState());
        assertEquals(TaskSystem.SELF, taskResource.getTaskSystem());
        assertEquals(SecurityClassification.PUBLIC, taskResource.getSecurityClassification());
        assertEquals("someTitle", taskResource.getTitle());
        assertNull(taskResource.getDescription());
        assertNull(taskResource.getNotes());
        assertNull(taskResource.getMajorPriority());
        assertNull(taskResource.getMinorPriority());
        assertNull(taskResource.getAssignee());
        assertEquals(false, taskResource.getAutoAssigned());
        assertNull(taskResource.getRoleCategory());
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
        assertEquals("tribunal-caseworker", taskResource.getTaskRoleResources().stream().findFirst().get().getRoleName());
    }

}
