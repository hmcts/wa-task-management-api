package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;

import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
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

    private CamundaObjectMapper camundaObjectMapper;

    private CamundaService camundaService;

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

        CamundaTask actualCamundaTask = camundaService.getTask(taskId);
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
            camundaService.getTask(taskId);
        });
    }

    @Test
    void should_get_variables() {

        final String caseId = randomUUID().toString();
        Map<String, CamundaVariable> processVariables = Map.of(
            CASE_ID.value(), new CamundaVariable(caseId, "String"),
            TASK_STATE.value(), new CamundaVariable(UNCONFIGURED, "String")
        );

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        when(camundaServiceApi.getVariables(serviceTokenId, taskId)).thenReturn(processVariables);

        Map<String, CamundaVariable> expectedProcessVariables =
            camundaService.getTaskVariables(taskId);

        assertNotNull(expectedProcessVariables);
        final CamundaVariable taskState = expectedProcessVariables.get(TASK_STATE.value());
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
            camundaService.getTaskVariables(taskId);
        });
    }

    @Test
    void should_add_process_variables() {

        HashMap<String, CamundaValue<String>> variableToAdd = new HashMap<>();
        variableToAdd.put("key3", CamundaValue.stringValue("value3"));

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(variableToAdd);

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        camundaService.addProcessVariables(taskId, variableToAdd);

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
            camundaService.addProcessVariables(taskId, variableToAdd);
        });
    }

    @ParameterizedTest
    @EnumSource(value = TaskState.class, names = {"UNASSIGNED", "ASSIGNED"})
    void should_assign_task(final TaskState taskState) {

        final String assigneeId = randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);
        boolean taskStateIsAssignedAlready = ASSIGNED.equals(taskState.value());
        camundaService.assignTask(taskId, assigneeId, taskStateIsAssignedAlready);

        verify(camundaServiceApi).assignTask(anyString(), anyString(), anyMap());
    }

    @Test
    void should_call_updateTaskStateTo_method_when_for_never_assigned_task() {
        final String assigneeId = randomUUID().toString();

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        boolean taskStateIsAssignedAlready = ASSIGNED.equals(UNASSIGNED.value());
        camundaService.assignTask(taskId, assigneeId, taskStateIsAssignedAlready);

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

        assertThrows(TaskAssignException.class, () -> {
            camundaService.assignTask(taskId, assigneeId, true);
        });
    }

    @Test
    void should_update_task_state_to() {

        HashMap<String, CamundaValue<String>> newTaskState = new HashMap<>();
        newTaskState.put("taskState", CamundaValue.stringValue(TaskState.ASSIGNED.value()));

        when(authTokenGenerator.generate()).thenReturn(serviceTokenId);

        final AddLocalVariableRequest addLocalVariableRequest = new AddLocalVariableRequest(newTaskState);

        camundaService.addProcessVariables(taskId, newTaskState);

        verify(camundaServiceApi).addLocalVariablesToTask(
            serviceTokenId,
            taskId,
            addLocalVariableRequest
        );
    }

    @BeforeEach
    public void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();

        TaskMapper taskMapper = new TaskMapper(camundaObjectMapper);
        camundaService = new CamundaService(
            camundaServiceApi,
            taskMapper,
            authTokenGenerator,
            camundaObjectMapper
        );

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
