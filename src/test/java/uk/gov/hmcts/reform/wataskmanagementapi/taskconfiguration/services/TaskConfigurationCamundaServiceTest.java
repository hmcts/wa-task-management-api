package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import feign.FeignException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ServerErrorException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

@ExtendWith(MockitoExtension.class)
class TaskConfigurationCamundaServiceTest {

    private TaskToConfigure testTaskToConfigure;

    private String taskId;

    private String serviceTokenId;

    @Mock
    private CamundaServiceApi camundaServiceApi;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private TaskConfigurationCamundaService taskConfigurationCamundaService;

    @Test
    void should_get_task() {

        final String processInstanceId = "processInstanceId";
        CamundaTask camundaTask = new CamundaTask(
            testTaskToConfigure.getId(),
            processInstanceId,
            testTaskToConfigure.getName()
        );

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        when(camundaServiceApi.getTask(serviceTokenId, taskId)).thenReturn(camundaTask);

        CamundaTask actualCamundaTask = taskConfigurationCamundaService.getTask(taskId);
        Assertions.assertEquals(actualCamundaTask.getName(), camundaTask.getName());
        Assertions.assertEquals(actualCamundaTask.getId(), camundaTask.getId());
        Assertions.assertEquals(actualCamundaTask.getProcessInstanceId(), camundaTask.getProcessInstanceId());
    }

    @Test
    void should_handle_resource_exception_get_task_is_retrieved() {

        final String processInstanceId = "processInstanceId";
        CamundaTask camundaTask = new CamundaTask(
            testTaskToConfigure.getId(),
            processInstanceId,
            testTaskToConfigure.getName()
        );

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        when(camundaServiceApi.getTask(serviceTokenId, taskId)).thenThrow(FeignException.class);

        assertThrows(ResourceNotFoundException.class, () -> {
            taskConfigurationCamundaService.getTask(taskId);
        });
    }

    @Test
    void should_get_variables() {

        final String caseId = randomUUID().toString();
        Map<String, CamundaValue<Object>> processVariables = Map.of(
            CASE_ID.value(), new CamundaValue<>(caseId, "String"),
            TASK_STATE.value(), new CamundaValue<>(UNCONFIGURED, "String")
        );

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        when(camundaServiceApi.getVariables(serviceTokenId, taskId)).thenReturn(processVariables);

        Map<String, CamundaValue<Object>> expectedProcessVariables =
            taskConfigurationCamundaService.getVariables(taskId);

        assertNotNull(expectedProcessVariables);
        final CamundaValue<Object> taskState = expectedProcessVariables.get("taskState");
        Assertions.assertEquals(taskState.getValue().toString(), UNCONFIGURED.toString());
        Assertions.assertEquals(expectedProcessVariables.get("caseId").getValue().toString(), caseId);
    }

    @Test
    void should_resource_exception_get_variables_retrieved() {

        final String caseId = randomUUID().toString();
        Map<String, CamundaValue<Object>> processVariables = Map.of(
            CASE_ID.value(), new CamundaValue<>(caseId, "String"),
            TASK_STATE.value(), new CamundaValue<>(UNCONFIGURED, "String")
        );

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        when(camundaServiceApi.getVariables(serviceTokenId, taskId)).thenThrow(FeignException.class);

        assertThrows(ResourceNotFoundException.class, () -> {
            taskConfigurationCamundaService.getVariables(taskId);
        });
    }

    @Test
    void should_add_process_variables() {

        HashMap<String, CamundaValue<String>> variableToAdd = new HashMap<>();
        variableToAdd.put("key3", CamundaValue.stringValue("value3"));

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(variableToAdd);

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        taskConfigurationCamundaService.addProcessVariables(taskId, variableToAdd);

        verify(camundaServiceApi).addLocalVariablesToTask(
            serviceTokenId,
            taskId,
            addLocalVariableRequest
        );
    }

    @Test
    void should_handle_resource_exception_when_process_variables_retrieved() {

        HashMap<String, CamundaValue<String>> variableToAdd = new HashMap<>();
        variableToAdd.put("key3", CamundaValue.stringValue("value3"));

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(variableToAdd);

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        doThrow(FeignException.class).when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

        assertThrows(ResourceNotFoundException.class, () -> {
            taskConfigurationCamundaService.addProcessVariables(taskId, variableToAdd);
        });
    }

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"UNASSIGNED", "ASSIGNED"})
    void should_assign_task(final TaskState taskState) {

        final String assigneeId = randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        taskConfigurationCamundaService.assignTask(taskId, assigneeId, taskState.value());

        verify(camundaServiceApi).assignTask(serviceTokenId, taskId, new AssigneeRequest(assigneeId));
    }

    @Test
    void should_call_updateTaskStateTo_method_when_assign_a_task_and_task_never_assigned() {
        final String assigneeId = randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        taskConfigurationCamundaService.assignTask(taskId, assigneeId, TaskState.UNASSIGNED.value());

        HashMap<String, CamundaValue<String>> newTaskState = new HashMap<>();
        newTaskState.put("taskState", CamundaValue.stringValue(TaskState.ASSIGNED.value()));

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(newTaskState);

        verify(camundaServiceApi, times(1))
            .addLocalVariablesToTask(
                serviceTokenId,
                taskId,
                addLocalVariableRequest
            );
    }

    @Test
    void should_handle_server_exception_when_task_is_assigned() {

        final String assigneeId = randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        doThrow(FeignException.class).when(camundaServiceApi).assignTask(any(), any(), any());

        assertThrows(ServerErrorException.class, () -> {
            taskConfigurationCamundaService.assignTask(taskId, assigneeId, "ASSIGNED");
        });
    }

    @Test
    void should_update_task_state_to() {

        HashMap<String, CamundaValue<String>> newTaskState = new HashMap<>();
        newTaskState.put("taskState", CamundaValue.stringValue(TaskState.ASSIGNED.value()));

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(newTaskState);

        taskConfigurationCamundaService.addProcessVariables(taskId, newTaskState);

        verify(camundaServiceApi).addLocalVariablesToTask(
            serviceTokenId,
            taskId,
            addLocalVariableRequest
        );
    }

    @BeforeEach
    public void setUp() {
        taskConfigurationCamundaService = new TaskConfigurationCamundaService(camundaServiceApi, authTokenGenerator);

        taskId = randomUUID().toString();

        serviceTokenId = randomUUID().toString();

        testTaskToConfigure = new TaskToConfigure(
            "taskId",
            "taskType",
            "caseId",
            "taskName"
        );
    }

}
