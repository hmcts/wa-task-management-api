package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaCftTaskStateUpdateException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder.WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY;

@ExtendWith(MockitoExtension.class)
class CamundaServiceTest extends CamundaHelpers {

    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK =
        "There was a problem fetching the variables for task with id: %s";
    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_ASSIGNING_THE_TASK_WITH_ID =
        "There was a problem assigning the task with id: %s";
    public static final String EXPECTED_MSG_COULD_NOT_COMPLETE_TASK_NOT_ASSIGNED =
        "Could not complete task with id: %s as task was not previously assigned";
    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID =
        "There was a problem fetching the task with id: %s";

    private static final WarningValues expectedWarningValues = new WarningValues(
        List.of(
            new Warning("Code1", "Text1"),
            new Warning("Code2", "Text2")
        )
    );

    @Mock
    private AuthTokenGenerator authTokenGenerator;
    @Mock
    private CamundaServiceApi camundaServiceApi;
    @Mock
    private PermissionEvaluatorService permissionEvaluatorService;
    private CamundaObjectMapper camundaObjectMapper;
    private CamundaService camundaService;
    private String taskId;

    @BeforeEach
    void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();

        TaskMapper taskMapper = new TaskMapper(camundaObjectMapper);
        camundaService = new CamundaService(
            camundaServiceApi,
            taskMapper,
            authTokenGenerator,
            permissionEvaluatorService,
            camundaObjectMapper
        );

        lenient().when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);
        taskId = UUID.randomUUID().toString();
    }

    private void verifyTaskStateUpdateWasCalled(String taskId, TaskState newTaskState) {
        Map<String, CamundaValue<String>> modifications = Map.of(
            CamundaVariableDefinition.TASK_STATE.value(), CamundaValue.stringValue(newTaskState.value())
        );
        verify(camundaServiceApi).addLocalVariablesToTask(
            BEARER_SERVICE_TOKEN,
            taskId,
            new AddLocalVariableRequest(modifications)
        );
    }

    @Nested
    @DisplayName("getMappedTask()")
    class GetMappedTask {
        @Test
        void getMappedTask_should_succeed() {
            CamundaTask mockedCamundaTask = createMockedUnmappedTask();
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);

            Task response = camundaService.getMappedTask(taskId, mockedVariables);

            assertNotNull(response);
            assertEquals("configured", response.getTaskState());
            assertEquals("someCaseName", response.getCaseName());
            assertEquals("someCaseType", response.getCaseTypeId());
            assertEquals("someCamundaTaskName", response.getName());
            assertEquals("someStaffLocationName", response.getLocationName());
            assertEquals(IDAM_USER_ID, response.getAssignee());
        }

        @Test
        void getMappedTask_should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown_by_get_task() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenThrow(FeignException.NotFound.class);

            assertThatThrownBy(() -> camundaService.getMappedTask(taskId, mockedVariables))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                    taskId
                ));

        }
    }

    @Nested
    @DisplayName("getUnmappedCamundaTask()")
    class GetUnmappedCamundaTask {
        @Test
        void should_succeed() {
            CamundaTask mockedCamundaTask = createMockedUnmappedTask();

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);

            CamundaTask response = camundaService.getUnmappedCamundaTask(taskId);

            assertNotNull(response);
            assertEquals(mockedCamundaTask, response);
        }

        @Test
        void should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown_by_get_task() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenThrow(FeignException.NotFound.class);

            assertThatThrownBy(() -> camundaService.getUnmappedCamundaTask(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                    taskId
                ));

        }
    }


    @Nested
    @DisplayName("getTaskVariables()")
    class GetTaskVariables {
        @Test
        void should_succeed() {
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            Map<String, CamundaVariable> response = camundaService.getTaskVariables(taskId);

            assertNotNull(response);
            assertEquals(mockedVariables, response);
        }

        @Test
        void should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown_by_get_task() {

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).getVariables(BEARER_SERVICE_TOKEN, taskId);

            assertThatThrownBy(() -> camundaService.getTaskVariables(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));

        }
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {

        @Test
        void should_cancel_task() {

            camundaService.cancelTask(taskId);
            verify(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());
        }

        @Test
        void should_cancel_task_when_search_history_throw_an_error() {
            camundaService.cancelTask(taskId);
            verify(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());
        }

        @Test
        void cancelTask_should_throw_a_task_cancel_exception_when_cancelling_task_fails() {

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());

            assertThatThrownBy(() -> camundaService.cancelTask(taskId))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");
        }

    }

    @Nested
    @DisplayName("claimTask()")
    class ClaimTask {
        @Test
        void claimTask_should_succeed() {

            camundaService.claimTask(taskId, IDAM_USER_ID);
            verify(camundaServiceApi, times(1))
                .claimTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());
            verifyTaskStateUpdateWasCalled(taskId, ASSIGNED);
        }

        @Test
        void claimTask_should_throw_task_claim_exception_when_updateTaskState_failed() {

            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

            assertThatThrownBy(() -> camundaService.claimTask(taskId, IDAM_USER_ID))
                .isInstanceOf(TaskClaimException.class)
                .hasNoCause()
                .hasMessage("Task Claim Error: "
                            + "Task claim failed. Unable to update task state to assigned.");
        }

        @Test
        void claimTask_should_throw_task_claim_exception_when_claim_failed() {

            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).claimTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                anyMap()
            );

            assertThatThrownBy(() -> camundaService.claimTask(taskId, IDAM_USER_ID))
                .isInstanceOf(TaskClaimException.class)
                .hasNoCause()
                .hasMessage("Task Claim Error: "
                            + "Task claim partially succeeded. "
                            + "The Task state was updated to assigned, but the Task could not be claimed.");
        }
    }

    @Nested
    @DisplayName("unclaimTask()")
    class UnclaimTask {
        @Test
        void unclaimTask_should_succeed() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(UNASSIGNED.value());
            camundaService.unclaimTask(taskId, taskHasUnassigned);

            verify(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            verify(camundaServiceApi)
                .unclaimTask(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            verifyNoMoreInteractions(camundaServiceApi);
        }


        @Test
        void unclaimTask_should_throw_a_task_unclaim_exception_when_state_update_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(UNASSIGNED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, taskHasUnassigned))
                .isInstanceOf(TaskUnclaimException.class)
                .hasNoCause()
                .hasMessage("Task Unclaim Error: Task unclaim failed. "
                            + "Unable to update task state to unassigned.");
        }

        @Test
        void unclaimTask_should_throw_a_task_unclaim_exception_when_unclaim_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(UNASSIGNED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).unclaimTask(any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, taskHasUnassigned))
                .isInstanceOf(TaskUnclaimException.class)
                .hasNoCause()
                .hasMessage("Task Unclaim Error: Task unclaim partially succeeded. "
                            + "The Task state was updated to unassigned, but the Task could not be unclaimed.");
        }
    }

    @Nested
    @DisplayName(("assignTask"))
    class AssignTask {
        @Test
        void assignTask_should_succeed() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean isTaskAssigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            camundaService.assignTask(taskId, IDAM_USER_ID, isTaskAssigned);

            verify(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            verify(camundaServiceApi)
                .assignTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());

            verifyNoMoreInteractions(camundaServiceApi);
        }


        @Test
        void assignTask_should_throw_a_task_assign_exception_when_state_update_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean isTaskAssigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.assignTask(taskId, IDAM_USER_ID, isTaskAssigned))
                .isInstanceOf(TaskAssignException.class)
                .hasNoCause()
                .hasMessage("Task Assign Error: Task assign failed. "
                            + "Unable to update task state to assigned.");
        }

        @Test
        void assignTask_should_throw_a_task_assign_exception_when_assign_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean isTaskAssigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).assignTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.assignTask(taskId, IDAM_USER_ID, isTaskAssigned))
                .isInstanceOf(TaskAssignException.class)
                .hasNoCause()
                .hasMessage("Task Assign Error: Task assign partially succeeded. "
                            + "The Task state was updated to assigned, but the Task could not be assigned.");
        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        @Test
        void completeTask_should_succeed() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasCompleted = mockedVariables.get("taskState").getValue().equals(COMPLETED.value());
            camundaService.completeTask(taskId, taskHasCompleted);

            verify(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            verify(camundaServiceApi)
                .completeTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), any(CompleteTaskVariables.class));

            verifyNoMoreInteractions(camundaServiceApi);
        }


        @Test
        void completeTask_should_throw_a_task_assign_exception_when_state_update_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasCompleted = mockedVariables.get("taskState").getValue().equals(COMPLETED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, taskHasCompleted))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. "
                            + "Unable to update task state to completed.");
        }

        @Test
        void completeTask_should_throw_a_task_assign_exception_when_assign_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasCompleted = mockedVariables.get("taskState").getValue().equals(COMPLETED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, taskHasCompleted))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete partially succeeded. "
                            + "The Task state was updated to completed, but the Task could not be completed.");
        }
    }

    @Nested
    @DisplayName("completeTaskById()")
    class CompleteTaskById {
        @Test
        void completeTaskById_should_succeed() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            camundaService.completeTaskById(taskId);


            verify(camundaServiceApi).getVariables(BEARER_SERVICE_TOKEN, taskId);

            verify(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            verify(camundaServiceApi)
                .completeTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), any(CompleteTaskVariables.class));

            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void completeTaskById_should_throw_a_resource_not_found_exception_when_get_variables_fails() {

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).getVariables(BEARER_SERVICE_TOKEN, taskId);

            assertThatThrownBy(() -> camundaService.completeTaskById(taskId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(
                    format("There was a problem fetching the variables for task with id: %s", taskId)
                );
        }

        @Test
        void completeTaskById_should_throw_a_task_assign_exception_when_state_update_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTaskById(taskId))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. "
                            + "Unable to update task state to completed.");
        }

        @Test
        void completeTaskById_should_throw_a_task_assign_exception_when_assign_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTaskById(taskId))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete partially succeeded. "
                            + "The Task state was updated to completed, but the Task could not be completed.");
        }
    }

    @Nested
    @DisplayName("deleteCftTaskState()")
    class DeleteCftTaskState {
        @Test
        void deleteCftTaskState_should_succeed() {

            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "someId",
                CFT_TASK_STATE.value(),
                "someValue"
            );

            when(camundaServiceApi.searchHistory(eq(BEARER_SERVICE_TOKEN), any()))
                .thenReturn(singletonList(historyVariableInstance));

            camundaService.deleteCftTaskState(taskId);

            verify(camundaServiceApi)
                .deleteVariableFromHistory(
                    BEARER_SERVICE_TOKEN,
                    historyVariableInstance.getId()
                );

            verify(camundaServiceApi, times(1))
                .deleteVariableFromHistory(
                    BEARER_SERVICE_TOKEN,
                    historyVariableInstance.getId()
                );

        }

        @Test
        void deleteCftTaskState_should_not_call_camunda_if_no_variable_found() {

            when(camundaServiceApi.searchHistory(eq(BEARER_SERVICE_TOKEN), any()))
                .thenReturn(emptyList());

            camundaService.deleteCftTaskState(taskId);

            verify(camundaServiceApi, times(0))
                .deleteVariableFromHistory(any(), any());

            verify(camundaServiceApi, never())
                .deleteVariableFromHistory(
                    BEARER_SERVICE_TOKEN,
                    "someId"
                );
        }

        @Test
        void deleteCftTaskState_should_throw_a_server_error_exception_when_historic_call_fails() {

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).searchHistory(eq(BEARER_SERVICE_TOKEN), any());

            assertThatThrownBy(() -> camundaService.deleteCftTaskState(taskId))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(
                    "There was a problem when deleting the historic cftTaskState"
                );
        }

        @Test
        void deleteCftTaskState_should_throw_a_server_error_exception_when_delete_call_fails() {

            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "someId",
                CFT_TASK_STATE.value(),
                "someValue"
            );

            when(camundaServiceApi.searchHistory(eq(BEARER_SERVICE_TOKEN), any()))
                .thenReturn(singletonList(historyVariableInstance));

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).deleteVariableFromHistory(eq(BEARER_SERVICE_TOKEN), any());

            assertThatThrownBy(() -> camundaService.deleteCftTaskState(taskId))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(
                    "There was a problem when deleting the historic cftTaskState"
                );
        }

    }


    @Nested
    @DisplayName("assignAndCompleteTask()")
    class AssignAndCompleteTask {

        @Test
        void should_complete_and_assign_task() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            camundaService.assignAndCompleteTask(
                taskId,
                IDAM_USER_ID,
                taskHasUnassigned
            );

            verifyTaskStateUpdateWasCalled(taskId, ASSIGNED);
            verify(camundaServiceApi).assignTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());
            verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
            verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
        }

        @Test
        void should_not_call_task_state_update_if_task_state_already_assigned() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            mockedVariables.put("taskState", new CamundaVariable(ASSIGNED.value(), "String"));
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            camundaService.assignAndCompleteTask(
                taskId,
                IDAM_USER_ID,
                taskHasUnassigned
            );

            Map<String, CamundaValue<String>> modifications = Map.of(
                CamundaVariableDefinition.TASK_STATE.value(), CamundaValue.stringValue(ASSIGNED.value())
            );

            verify(camundaServiceApi, times(0)).addLocalVariablesToTask(
                BEARER_SERVICE_TOKEN,
                taskId,
                new AddLocalVariableRequest(modifications)
            );

            verify(camundaServiceApi).assignTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());
            verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
            verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
        }

        @Test
        void should_throw_a_task_assign_and_complete_exception_when_addLocalVariablesToTask_fails() {
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    taskHasUnassigned
                ))
                .isInstanceOf(TaskAssignAndCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Assign and Complete Error: "
                            + "Task assign and complete partially succeeded. "
                            + "The Task was assigned to the user making the request but the "
                            + "Task could not be completed.");

        }

        @Test
        void should_throw_a_task_assign_and_complete_exception_when_assignTask_fails() {
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                anyMap()
            );

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    taskHasUnassigned
                ))
                .isInstanceOf(TaskAssignAndCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Assign and Complete Error: Unable to assign the Task to the current user.");

        }

        @Test
        void should_throw_a_task_assign_and_complete_exception_when_completing_task_fails() {
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            boolean taskHasUnassigned = mockedVariables.get("taskState").getValue().equals(ASSIGNED.value());
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    taskHasUnassigned
                ))
                .isInstanceOf(TaskAssignAndCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Assign and Complete Error: "
                            + "Task assign and complete partially succeeded. "
                            + "The Task was assigned to the user making the request, "
                            + "the task state was also updated to completed, but he Task could not be completed.");
        }
    }

    @Nested
    @DisplayName("getTaskCount()")
    class TaskCount {

        @Test
        void should_return_task_count() {
            final CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);

            when(camundaServiceApi.getTaskCount(
                BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(new CamundaTaskCount(1));

            assertEquals(1, camundaService.getTaskCount(camundaSearchQuery));
        }

        @Test
        void getTaskCount_throw_a_server_error_exception_when_camunda_task_count_fails() {
            final CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).getTaskCount(eq(BEARER_SERVICE_TOKEN), any());

            assertThatThrownBy(() -> camundaService.getTaskCount(camundaSearchQuery))
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem retrieving task count")
                .hasCauseInstanceOf(FeignException.class);
        }
    }

    @Nested
    @DisplayName("searchWithCriteria()")
    class SearchWithCriteria {

        @Test
        void searchWithCriteria_should_succeed() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );


            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            mockCamundaGetAllVariables();

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("configured", results.get(0).getTaskState());
            assertEquals(dueDate, results.get(0).getDueDate());
            assertEquals("someCaseName", results.get(0).getCaseName());
            assertEquals("someCaseType", results.get(0).getCaseTypeId());
            assertEquals("someTaskName", results.get(0).getName());
            assertNotNull(results.get(0).getLocation());
            assertEquals("someStaffLocationName", results.get(0).getLocationName());
            assertNotNull(results.get(0).getAssignee());
            assertEquals("someAssignee", results.get(0).getAssignee());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_user_did_not_have_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            mockCamundaGetAllVariables();


            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_task_did_not_have_variables() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            mockCamundaGetAllVariables();

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_when_multiple_tasks_returned_and_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            CamundaTask camundaTask2 = new CamundaTask(
                "someTaskId2",
                "someTaskName2",
                "someAssignee2",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey2",
                "someProcessInstanceId2"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("configured", results.get(0).getTaskState());
            assertEquals("configured", results.get(1).getTaskState());
            assertEquals(dueDate, results.get(0).getDueDate());
            assertEquals(dueDate, results.get(1).getDueDate());
            assertEquals("someCaseName", results.get(0).getCaseName());
            assertEquals("someCaseName", results.get(1).getCaseName());
            assertEquals("someCaseType", results.get(0).getCaseTypeId());
            assertEquals("someCaseType", results.get(1).getCaseTypeId());
            assertEquals("someTaskName", results.get(0).getName());
            assertEquals("someTaskName2", results.get(1).getName());
            assertNotNull(results.get(0).getLocation());
            assertNotNull(results.get(1).getLocation());
            assertEquals("someStaffLocationName", results.get(0).getLocationName());
            assertEquals("someStaffLocationName", results.get(1).getLocationName());
            assertNotNull(results.get(0).getAssignee());
            assertNotNull(results.get(1).getAssignee());
            assertEquals("someAssignee", results.get(0).getAssignee());
            assertEquals("someAssignee2", results.get(1).getAssignee());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_when_multiple_tasks_returned_and_only_one_with_variables_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            CamundaTask camundaTask2 = new CamundaTask(
                "someTaskId2",
                "someTaskName2",
                "someAssignee2",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey2",
                "someProcessInstanceId2"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId", "someTaskId"));

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("configured", results.get(0).getTaskState());
            assertEquals(dueDate, results.get(0).getDueDate());
            assertEquals("someCaseName", results.get(0).getCaseName());
            assertEquals("someCaseType", results.get(0).getCaseTypeId());
            assertEquals("someTaskName", results.get(0).getName());
            assertNotNull(results.get(0).getLocation());
            assertEquals("someStaffLocationName", results.get(0).getLocationName());
            assertNotNull(results.get(0).getAssignee());
            assertEquals("someAssignee", results.get(0).getAssignee());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_and_return_empty_list_when_multiple_tasks_returned_and_not_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId1"
            );

            CamundaTask camundaTask2 = new CamundaTask(
                "someTaskId2",
                "someTaskName2",
                "someAssignee2",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey2",
                "someProcessInstanceId2"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", asList("someProcessInstanceId1", "someProcessInstanceId2"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", asList("someProcessInstanceId1", "someProcessInstanceId2"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_when_multiple_process_variables_returned_and_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("configured", results.get(0).getTaskState());
            assertEquals(dueDate, results.get(0).getDueDate());
            assertEquals("someCaseName", results.get(0).getCaseName());
            assertEquals("someCaseType", results.get(0).getCaseTypeId());
            assertEquals("someTaskName", results.get(0).getName());
            assertNotNull(results.get(0).getLocation());
            assertEquals("someStaffLocationName", results.get(0).getLocationName());
            assertNotNull(results.get(0).getAssignee());
            assertEquals("someAssignee", results.get(0).getAssignee());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_return_empty_list_when_multiple_process_variables_returned_and_user_did_not_have_permissions() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);

            List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_local_variables_call_fails() {

            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(mock(CamundaTask.class)));

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            )).thenReturn(singletonList(mock(
                CamundaTask.class)));

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).getAllVariables(eq(BEARER_SERVICE_TOKEN), any());

            assertThatThrownBy(() ->
                camundaService.searchWithCriteria(
                    camundaSearchQueryMock, 0, 1, accessControlResponse, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage("There was a problem performing the search");
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_searchWithCriteria_returns_emptyList() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);

            List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(emptyList());

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock,
                0,
                1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_getAllVariables_returns_emptyList() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "someAssignee",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(emptyList());

            List<Task> results = camundaService.searchWithCriteria(
                camundaSearchQueryMock, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaServiceApi, times(1)).searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("processInstanceIdIn", singletonList("someProcessInstanceId"),
                        "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                    )
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_search_call_fails() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                eq(BEARER_SERVICE_TOKEN), anyInt(), anyInt(), any()))
                .thenThrow(FeignException.class);

            assertThatThrownBy(() ->
                camundaService.searchWithCriteria(
                    camundaSearchQueryMock,
                    0,
                    1,
                    accessControlResponse,
                    permissionsRequired
                )
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem performing the search")
                .hasCauseInstanceOf(FeignException.class);
        }

        private void mockCamundaGetAllVariables() {
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of(
                    "processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId", "someTaskId"));
        }
    }

    @Nested
    @DisplayName("searchWithCriteriaAndNoPagination()")
    class AutoCompleteTask {

        @Test
        void should_succeed() {

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            List<CamundaTask> camundaTasks = singletonList(createMockedUnmappedTask());
            when(camundaServiceApi.searchWithCriteriaAndNoPagination(
                eq(BEARER_SERVICE_TOKEN), anyMap())).thenReturn(camundaTasks);
            List<CamundaTask> response = camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery);

            assertNotNull(response);
            assertEquals(camundaTasks, response);
        }

        @Test
        void should_throw_a_server_error_exception_when_search_fails() {

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).searchWithCriteriaAndNoPagination(eq(BEARER_SERVICE_TOKEN), anyMap());

            assertThatThrownBy(() -> camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage("There was a problem performing the search");

        }
    }


    @Nested
    @DisplayName("evaluateTaskCompletionDmn()")
    class EvaluateTaskCompletionDmn {

        @ParameterizedTest
        @CsvSource(
            value = {
                "ia, asylum, wa-task-completion-ia-asylum",
                "wa, wacasetype, wa-task-completion-wa-wacasetype",
            }
        )
        void should_succeed_for_different_jurisdictions(String jurisdiction, String caseType, String dmn) {

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                jurisdiction,
                caseType);

            List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
                "taskType", new CamundaVariable("reviewTheAppeal", "String"),
                "completionMode", new CamundaVariable("Auto", "String")
            ));

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(dmn),
                eq(jurisdiction),
                anyMap()))
                .thenReturn(mockedResponse);

            List<Map<String, CamundaVariable>> response = camundaService.evaluateTaskCompletionDmn(searchEventAndCase);
            assertEquals(mockedResponse, response);
        }

        @ParameterizedTest
        @CsvSource(
            value = {
                "ia, asylum, wa-task-completion-ia-asylum",
                "wa, wacasetype, wa-task-completion-wa-wacasetype",
            }
        )
        void should_throw_a_server_error_exception_when_search_fails(String jurisdiction, String caseType, String dmn) {

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                jurisdiction,
                caseType);


            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).evaluateDMN(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(dmn),
                    eq(jurisdiction),
                    anyMap());

            assertThatThrownBy(() -> camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage("There was a problem evaluating DMN");

        }
    }


    @Nested
    @DisplayName("getVariableValue()")
    class GetVariableValue {

        @Test
        void should_return_camunda_variable() {

            Map<String, CamundaVariable> mockedVariables = new HashMap<>();
            mockedVariables.put("caseId", new CamundaVariable("00000", "String"));
            mockedVariables.put("jurisdiction", new CamundaVariable("IA", "String"));

            String response = camundaService.getVariableValue(mockedVariables.get(JURISDICTION.value()), String.class);

            assertNotNull(response);
            assertEquals(mockedVariables.get("jurisdiction").getValue(), response);

        }

        @Test
        void should_return_null_when_variable_not_found() {

            Map<String, CamundaVariable> mockedVariables = new HashMap<>();
            mockedVariables.put("caseId", new CamundaVariable("00000", "String"));
            mockedVariables.put("jurisdiction", new CamundaVariable("IA", "String"));

            String response = camundaService.getVariableValue(mockedVariables.get(LOCATION.value()), String.class);

            assertNull(response);

        }

    }

    @Nested
    @DisplayName("performSearchAction()")
    class PerformSearchAction {

        List<CamundaTask> camundaTasks;
        List<RoleAssignment> roleAssignment;
        List<PermissionTypes> permissionsRequired;
        AccessControlResponse accessControlResponse;

        @BeforeEach
        void setup() {

            camundaTasks = List.of(createMockedUnmappedTask());
            roleAssignment = singletonList(mock(RoleAssignment.class));
            permissionsRequired = singletonList(READ);
            accessControlResponse = new AccessControlResponse(mock(UserInfo.class), roleAssignment);

        }

        @Test
        void should_return_task_list() {

            mockCamundaGetAllVariables();

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> actualTask = camundaService.performSearchAction(camundaTasks,
                accessControlResponse,
                permissionsRequired);

            assertEquals("java.util.ArrayList", actualTask.getClass().getCanonicalName());
            assertEquals(1, actualTask.size());
            assertEquals(CONFIGURED.value(), actualTask.get(0).getTaskState());

        }

        @Test
        void should_return_task_list_with_warnings() {

            String taskId = "someCamundaTaskId";
            mockCamundaGetAllVariablesWithWarning(taskId);

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> actualTask = camundaService.performSearchAction(camundaTasks,
                accessControlResponse,
                permissionsRequired);

            assertEquals(1, actualTask.size());
            assertEquals(taskId, actualTask.get(0).getId());
            assertEquals(CONFIGURED.value(), actualTask.get(0).getTaskState());
            assertTrue(actualTask.get(0).getWarnings());
            assertEquals(expectedWarningValues, actualTask.get(0).getWarningList());

        }

        @Test
        void should_return_task_list_with_warnings_permission_is_false() {

            String taskId = "someCamundaTaskId";
            mockCamundaGetAllVariablesWithWarning(taskId);

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> actualTask = camundaService.performSearchAction(camundaTasks,
                accessControlResponse,
                permissionsRequired);

            assertEquals(0, actualTask.size());

        }

        @Test
        void should_return_task_list_with_warnings_and_taskId_is_null() {

            mockCamundaGetAllVariablesWithWarning(null);

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> actualTask = camundaService.performSearchAction(camundaTasks,
                accessControlResponse,
                permissionsRequired);

            assertEquals(1, actualTask.size());
            assertEquals("someCamundaTaskId", actualTask.get(0).getId());
            assertNull(actualTask.get(0).getTaskState());
            assertTrue(actualTask.get(0).getWarnings());
            assertEquals(expectedWarningValues, actualTask.get(0).getWarningList());

        }

        private void mockCamundaGetAllVariables() {
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of(
                    "processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId", "someTaskId"));
        }

        private void mockCamundaGetAllVariablesWithWarning(String taskId) {
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of(
                    "processInstanceIdIn", singletonList("someProcessInstanceId"),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponseWithWarning("someProcessInstanceId", taskId));
        }

    }

    @Nested
    @DisplayName("updateCftTaskState()")
    class UpdateCftTaskState {

        @Test
        void should_call_addLocalVariablesToTask_when_a_task_updated() {

            camundaService.updateCftTaskState(taskId, COMPLETED);

            verify(camundaServiceApi, times(1))
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );


        }

        @Test
        void should_throw_exception_when_an_error_occurred() {

            doThrow(CamundaCftTaskStateUpdateException.class)
                .when(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            assertThatThrownBy(() -> camundaService.updateCftTaskState(taskId, COMPLETED))
                .isInstanceOf(ServerErrorException.class)
                .hasMessageStartingWith("There was a problem when updating the cftTaskState");

        }

    }

    @Nested
    @DisplayName("getCftTaskState()")
    class GetCftTaskState {

        @Test
        void should_return_expected_cft_task_state() {

            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "someId",
                CFT_TASK_STATE.value(),
                "someValue"
            );

            Map<String, Object> body = Map.of(
                "variableName", CFT_TASK_STATE.value(),
                "taskIdIn", singleton(taskId)
            );

            when(camundaServiceApi.searchHistory(BEARER_SERVICE_TOKEN, body))
                .thenReturn(singletonList(historyVariableInstance));

            boolean actualIsCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

            assertTrue(actualIsCftTaskStateExist);

            verify(camundaServiceApi).searchHistory(BEARER_SERVICE_TOKEN, body);

            verify(camundaServiceApi, times(1))
                .searchHistory(
                    BEARER_SERVICE_TOKEN,
                    body
                );

        }

        @Test
        void should_return_null_when_search_response_is_null() {

            Map<String, Object> body = Map.of(
                "variableName", CFT_TASK_STATE.value(),
                "taskIdIn", singleton(taskId)
            );

            when(camundaServiceApi.searchHistory(BEARER_SERVICE_TOKEN, body))
                .thenReturn(null);

            boolean actualIsCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

            assertFalse(actualIsCftTaskStateExist);

        }

        @Test
        void should_return_null_when_search_response_is_empty_list() {

            Map<String, Object> body = Map.of(
                "variableName", CFT_TASK_STATE.value(),
                "taskIdIn", singleton(taskId)
            );

            when(camundaServiceApi.searchHistory(BEARER_SERVICE_TOKEN, body))
                .thenReturn(emptyList());

            boolean actualIsCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

            assertFalse(actualIsCftTaskStateExist);

        }

        @Test
        void should_return_null_when_cft_task_state_variable_not_exists_in_response() {

            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "someId",
                "someVariable",
                "someValue"
            );

            Map<String, Object> body = Map.of(
                "variableName", CFT_TASK_STATE.value(),
                "taskIdIn", singleton(taskId)
            );

            when(camundaServiceApi.searchHistory(BEARER_SERVICE_TOKEN, body))
                .thenReturn(singletonList(historyVariableInstance));

            boolean actualIsCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

            assertFalse(actualIsCftTaskStateExist);

        }

        @Test
        void should_throw_task_cancel_exception_when_camunda_client_throws_feign_exception() {

            Map<String, Object> body = Map.of(
                "variableName", CFT_TASK_STATE.value(),
                "taskIdIn", singleton(taskId)
            );

            doThrow(FeignException.class)
                .when(camundaServiceApi)
                .searchHistory(
                    BEARER_SERVICE_TOKEN,
                    body
                );

            assertThatThrownBy(() -> camundaService.isCftTaskStateExistInCamunda(taskId))
                .isInstanceOf(TaskCancelException.class)
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verify(camundaServiceApi, times(1))
                .searchHistory(
                    BEARER_SERVICE_TOKEN,
                    body
                );

        }


    }
}
