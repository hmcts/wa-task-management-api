package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
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
            camundaService.unclaimTask(taskId, mockedVariables);

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

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, mockedVariables))
                .isInstanceOf(TaskUnclaimException.class)
                .hasNoCause()
                .hasMessage("Task Unclaim Error: Task unclaim failed. "
                            + "Unable to update task state to unassigned.");
        }

        @Test
        void unclaimTask_should_throw_a_task_unclaim_exception_when_unclaim_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).unclaimTask(any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, mockedVariables))
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
            camundaService.assignTask(taskId, IDAM_USER_ID, mockedVariables);

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

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.assignTask(taskId, IDAM_USER_ID, mockedVariables))
                .isInstanceOf(TaskAssignException.class)
                .hasNoCause()
                .hasMessage("Task Assign Error: Task assign failed. "
                            + "Unable to update task state to assigned.");
        }

        @Test
        void assignTask_should_throw_a_task_assign_exception_when_assign_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).assignTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.assignTask(taskId, IDAM_USER_ID, mockedVariables))
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
            camundaService.completeTask(taskId, mockedVariables);

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

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, mockedVariables))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. "
                            + "Unable to update task state to completed.");
        }

        @Test
        void completeTask_should_throw_a_task_assign_exception_when_assign_task_fails() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, mockedVariables))
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
    @DisplayName("assignAndCompleteTask()")
    class AssignAndCompleteTask {

        @Test
        void should_complete_and_assign_task() {

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            camundaService.assignAndCompleteTask(
                taskId,
                IDAM_USER_ID,
                mockedVariables
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

            camundaService.assignAndCompleteTask(
                taskId,
                IDAM_USER_ID,
                mockedVariables
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

            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    mockedVariables
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

            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                anyMap()
            );

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    mockedVariables
                ))
                .isInstanceOf(TaskAssignAndCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Assign and Complete Error: Unable to assign the Task to the current user.");

        }

        @Test
        void should_throw_a_task_assign_and_complete_exception_when_completing_task_fails() {
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

            assertThatThrownBy(() ->
                camundaService.assignAndCompleteTask(
                    taskId,
                    IDAM_USER_ID,
                    mockedVariables
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

}
