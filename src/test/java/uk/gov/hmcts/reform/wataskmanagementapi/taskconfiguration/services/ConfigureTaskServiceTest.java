package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

class ConfigureTaskServiceTest {

    private static final String ASSIGNEE = "assignee1";
    private TaskToConfigure task;
    private TaskConfigurationCamundaService camundaService;
    private ConfigureTaskService configureTaskService;
    private TaskConfigurator taskVariableExtractor;
    private TaskAutoAssignmentService autoAssignmentService;
    private CFTTaskMapper cftTaskMapper;

    @BeforeEach
    void setup() {
        camundaService = mock(TaskConfigurationCamundaService.class);
        taskVariableExtractor = mock(TaskConfigurator.class);
        autoAssignmentService = mock(TaskAutoAssignmentService.class);
        cftTaskMapper = mock(CFTTaskMapper.class);
        configureTaskService = new ConfigureTaskService(
            camundaService,
            Collections.singletonList(taskVariableExtractor),
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

}
