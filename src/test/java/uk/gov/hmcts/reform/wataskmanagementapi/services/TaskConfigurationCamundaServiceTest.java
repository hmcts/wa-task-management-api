package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskClaimException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskUnclaimException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_COMPLETION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder.WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY;

class TaskConfigurationCamundaServiceTest extends TaskConfigurationCamundaServiceBaseTest {

    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK =
        "There was a problem fetching the variables for task with id: %s";

    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_ASSIGNING_THE_TASK_WITH_ID =
        "There was a problem assigning the task with id: %s";

    public static final String EXPECTED_MSG_COULD_NOT_COMPLETE_TASK_NOT_ASSIGNED =
        "Could not complete task with id: %s as task was not previously assigned";

    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID =
        "There was a problem fetching the task with id: %s";

    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    private Map<String, CamundaVariable> mockVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("SC", "String"));
        variables.put("title", new CamundaVariable("some_title", "String"));
        variables.put("executionType", new CamundaVariable("some_executionType", "String"));
        variables.put("taskSystem", new CamundaVariable("some_taskSystem", "String"));
        variables.put("jurisdiction", new CamundaVariable("some_jurisdiction", "String"));
        variables.put("region", new CamundaVariable("some_region", "String"));
        variables.put("appealType", new CamundaVariable("some_appealType", "String"));
        variables.put("autoAssigned", new CamundaVariable("false", "Boolean"));
        variables.put("assignee", new CamundaVariable("uid", "String"));
        return variables;
    }

    private CamundaTask createMockCamundaTaskWithNoAssignee() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            null,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    private CamundaTask createMockCamundaTask() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            IDAM_USER_ID,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    private List<Map<String, CamundaVariable>> mockDMN() {
        return singletonList(
            Map.of(
                "completionMode", new CamundaVariable("Auto", "String"),
                "taskType", new CamundaVariable("reviewTheAppeal", "String")
            ));
    }

    private List<Map<String, CamundaVariable>> mockDMNWithEmptyRow() {
        List<Map<String, CamundaVariable>> dmnResult = new ArrayList<>();
        final Map<String, CamundaVariable> completionMode = Map.of(
            "completionMode", new CamundaVariable("Auto", "String"),
            "taskType", new CamundaVariable("reviewTheAppeal", "String")
        );
        dmnResult.add(completionMode);
        dmnResult.add(emptyMap());

        return dmnResult;
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

    private List<CamundaVariableInstance> mockedVariablesResponse(String processInstanceId, String taskId) {
        Map<String, CamundaVariable> mockVariables = mockVariables();

        return mockVariables.keySet().stream()
            .map(
                mockVarKey ->
                    new CamundaVariableInstance(
                        mockVariables.get(mockVarKey).getValue(),
                        mockVariables.get(mockVarKey).getType(),
                        mockVarKey,
                        processInstanceId,
                        taskId
                    ))
            .collect(Collectors.toList());

    }

    private List<CamundaVariableInstance> mockedVariablesResponseForMultipleProcessIds() {
        List<CamundaVariableInstance> variablesForProcessInstance1 =
            mockedVariablesResponse("someProcessInstanceId", "someTaskId");
        List<CamundaVariableInstance> variablesForProcessInstance2 =
            mockedVariablesResponse("someProcessInstanceId2", "someTaskId2");

        return Stream.of(variablesForProcessInstance1, variablesForProcessInstance2)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("getTask()")
    class GetTask {
        @Test
        void getTask_should_succeed() {

            String taskId = UUID.randomUUID().toString();
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            CamundaTask mockedCamundaTask = createMockCamundaTask();
            Map<String, CamundaVariable> mockedVariables = mockVariables();
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(true);

            Task response = camundaService.getTask(taskId, roleAssignment, permissionsRequired);

            assertNotNull(response);
            assertEquals("configured", response.getTaskState());
            assertEquals("someCaseName", response.getCaseName());
            assertEquals("someCaseType", response.getCaseTypeId());
            assertEquals("someCamundaTaskName", response.getName());
            assertEquals("someStaffLocationName", response.getLocationName());
            assertEquals(IDAM_USER_ID, response.getAssignee());
        }

        @Test
        void getTask_should_throw_insufficient_permissions_exception_when_has_access_returns_false() {

            String taskId = UUID.randomUUID().toString();
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(() -> camundaService.getTask(taskId, roleAssignment, permissionsRequired))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        }

        @Test
        void getTask_throw_a_resource_not_found_exception_exception_when_feign_exception_is_thrown_by_get_variables() {

            String taskId = UUID.randomUUID().toString();
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            List<PermissionTypes> permissionsRequired = singletonList(READ);


            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", "some exception message")
                );

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenThrow(exception);

            assertThatThrownBy(() -> camundaService.getTask(taskId, roleAssignment, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));

        }

        @Test
        void getTask_should_throw_a_resource_not_found_exception_when_feign_exception_is_thrown_by_get_task() {

            String taskId = UUID.randomUUID().toString();
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            Map<String, CamundaVariable> mockedVariables = mockVariables();
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(true);
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenThrow(FeignException.NotFound.class);

            assertThatThrownBy(() -> camundaService.getTask(taskId, roleAssignment, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                    taskId
                ));

        }
    }

    @Nested
    @DisplayName("searchWithCriteria()")
    class SearchWithCriteria {

        @Test
        void given_task_assigneeId_equals_to_userId_then_add_task_once() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            UserInfo userInfoMock = mock(UserInfo.class);
            String someAssignee = "someAssignee";
            final AccessControlResponse accessControlResponse = new AccessControlResponse(userInfoMock, roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
            final SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            final CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            final ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                someAssignee,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
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
            assertEquals(someAssignee, results.get(0).getAssignee());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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
            verifyNoMoreInteractions(permissionEvaluatorService);
        }

        @Test
        void searchWithCriteria_should_succeed() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
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
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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
        @MockitoSettings(strictness = Strictness.LENIENT)
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_query_was_null() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(null);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_user_did_not_have_sufficient_permission() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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
        @MockitoSettings(strictness = Strictness.LENIENT)
        void searchWithCriteria_should_succeed_and_return_empty_list_if_task_did_not_have_variables() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            final AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            final List<PermissionTypes> permissionsRequired = singletonList(READ);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            mockCamundaGetAllVariables();

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
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
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
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
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
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
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(mock(CamundaTask.class)));

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1,
                camundaSearchQueryMock.getQueries()
            )).thenReturn(singletonList(mock(
                CamundaTask.class)));

            when(camundaServiceApi.getAllVariables(eq(BEARER_SERVICE_TOKEN), any())).thenThrow(
                new TestFeignClientException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", "some exception message")
                )
            );

            assertThatThrownBy(() ->
                                   camundaService.searchWithCriteria(
                                       searchTaskRequest, 0, 1, accessControlResponse, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(TestFeignClientException.class)
                .hasMessage("There was a problem performing the search");
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_searchWithCriteria_returns_emptyList() {
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);

            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteriaAndPagination(
                BEARER_SERVICE_TOKEN, 0, 1, camundaSearchQueryMock.getQueries()))
                .thenReturn(emptyList());

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
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
                searchTaskRequest, 0, 1,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
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

            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);


            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);

            when(camundaServiceApi.searchWithCriteriaAndPagination(
                eq(BEARER_SERVICE_TOKEN), anyInt(), anyInt(), any()))
                .thenThrow(FeignException.class);

            assertThatThrownBy(() ->
                                   camundaService.searchWithCriteria(
                                       searchTaskRequest, 0, 1, accessControlResponse, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem performing the search")
                .hasCauseInstanceOf(FeignException.class);
        }

        @Test
        void should_succeed_and_return_empty_array_when_wrong_jurisdiction_is_passed() {
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, PermissionTypes.MANAGE);
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "caseId", "eventId", "IA", "WrongCaseType");

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo,
                singletonList(mockedRoleAssignment)
            );


            lenient().when(authTokenGenerator.generate()).thenReturn(String.valueOf(true));
            GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            assertTrue(response.getTasks().isEmpty());
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
    @DisplayName("getTaskCount()")
    class TaskCount {

        @Test
        void should_return_task_count() {
            final SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            final CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);

            when(camundaServiceApi.getTaskCount(
                BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(new CamundaTaskCount(1));

            assertEquals(1, camundaService.getTaskCount(searchTaskRequest));
        }

        @Test
        void getTaskCount_throw_a_server_error_exception_when_camunda_task_count_fails() {
            final SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            final CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);

            when(camundaServiceApi.getTaskCount(
                BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenThrow(FeignException.class);

            assertThatThrownBy(() ->
                                   camundaService.getTaskCount(
                                       searchTaskRequest)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem retrieving task count")
                .hasCauseInstanceOf(FeignException.class);
        }
    }

    @Nested
    @DisplayName("claimTask()")
    class ClaimTask {
        @Test
        void claimTask_should_succeed() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(true);

            camundaService.claimTask(taskId, accessControlResponse, permissionsRequired);
            verify(camundaServiceApi, times(1))
                .claimTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());
            verifyTaskStateUpdateWasCalled(taskId, TaskState.ASSIGNED);
        }

        @Test
        void claimTask_should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception)
                .when(camundaServiceApi).getVariables(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            assertThatThrownBy(() -> camundaService.claimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));
        }

        @Test
        void claimTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(false);


            assertThatThrownBy(() -> camundaService.claimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
        }

        @Test
        void claimTask_should_throw__when_updateTaskState_failed() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(true);


            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

            assertThatThrownBy(() -> camundaService.claimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskClaimException.class)
                .hasNoCause()
                .hasMessage("Task Claim Error: "
                            + "Task claim failed. Unable to update task state to assigned.");
        }
    }

    @Nested
    @DisplayName("unclaimTask()")
    class UnclaimTask {
        @Test
        void unclaimTask_should_succeed_when_same_user() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = new UserInfo("email", "someCamundaTaskAssignee",
                                                   emptyList(), "name", "givenName", "familyName"
            );

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);
            CamundaTask mockedCamundaTask = createMockCamundaTask();
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                mockedCamundaTask.getAssignee(),
                mockedUserInfo.getUid(),
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(true);


            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );
            camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired);
            verify(camundaServiceApi)
                .unclaimTask(eq(BEARER_SERVICE_TOKEN), eq(taskId));
            verify(camundaServiceApi)
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any()
                );

            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void unclaimTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

            String taskId = UUID.randomUUID().toString();
            String exceptionMessage = "some exception message";
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);

            List<PermissionTypes> permissionsRequired = List.of(MANAGE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(UserInfo.builder().build(), singletonList(mockedRoleAssignment));

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    exceptionMessage
                );

            doThrow(exception)
                .when(camundaServiceApi).getVariables(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));
        }

        @Test
        void unclaimTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);

            UserInfo mockedUserInfo = new UserInfo("email", "someCamundaTaskAssignee",
                                                   emptyList(), "name", "givenName", "familyName"
            );

            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            CamundaTask mockedCamundaTask = createMockCamundaTask();
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                mockedCamundaTask.getAssignee(),
                mockedUserInfo.getUid(),
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
        }

        @Test
        void unclaimTask_should_throw_a_task_unclaim_exception_when_unclaim_task_fails() {
            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            UserInfo mockedUserInfo = new UserInfo("email", "someCamundaTaskAssignee",
                                                   emptyList(), "name", "givenName", "familyName"
            );

            CamundaTask mockedCamundaTask = createMockCamundaTask();
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                mockedCamundaTask.getAssignee(),
                mockedUserInfo.getUid(),
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(true);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).unclaimTask(any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskUnclaimException.class)
                .hasNoCause()
                .hasMessage("Task Unclaim Error: Task unclaim partially succeeded. "
                            + "The Task state was updated to unassigned, but the Task could not be unclaimed.");
        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        UserInfo mockedUserInfo;
        String taskId;
        RoleAssignment mockedRoleAssignment;
        Map<String, CamundaVariable> mockedVariables;
        List<PermissionTypes> permissionsRequired;
        AccessControlResponse accessControlResponse;
        CompletionOptions mockedCompletionOptions;
        CamundaTask mockedCamundaTask;

        @BeforeEach
        void beforeEach() {
            taskId = UUID.randomUUID().toString();
            mockedRoleAssignment = mock(RoleAssignment.class);
            mockedVariables = mockVariables();
            mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            mockedCompletionOptions = mock(CompletionOptions.class);
            mockedCamundaTask = createMockCamundaTask();
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);

            accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            permissionsRequired = asList(OWN, EXECUTE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                mockedCamundaTask.getAssignee(),
                IDAM_USER_ID,
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);
        }

        @Test
        void should_complete_task() {

            camundaService.completeTask(taskId, accessControlResponse, permissionsRequired);

            Map<String, CamundaValue<String>> modifications = new HashMap<>();
            modifications.put("taskState", CamundaValue.stringValue("completed"));
            verify(camundaServiceApi).addLocalVariablesToTask(
                BEARER_SERVICE_TOKEN,
                taskId,
                new AddLocalVariableRequest(modifications)
            );
            verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
            verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void should_fail_and_return_resource_not_found_exception_if_task_did_not_exist() {

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenThrow(FeignException.NotFound.class);

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                    taskId
                ));
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void should_fail_and_throw_task_state_incorrect_exception_if_task_was_not_assigned() {

            mockedCamundaTask = createMockCamundaTaskWithNoAssignee();
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskStateIncorrectException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_COULD_NOT_COMPLETE_TASK_NOT_ASSIGNED,
                    taskId
                ));
        }

        @Test
        void completeTask_does_not_call_camunda_task_state_update_complete_if_task_already_complete() {
            mockedVariables.put("taskState", new CamundaVariable(COMPLETED.value(), "String"));

            camundaService.completeTask(taskId, accessControlResponse, permissionsRequired);

            verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void completeTask_should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception)
                .when(camundaServiceApi).getVariables(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));

        }

        @Test
        void completeTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                mockedCamundaTask.getAssignee(),
                IDAM_USER_ID,
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
        }

        @Test
        void completeTask_should_throw_a_task_complete_exception_when_addLocalVariablesToTask_fails() {

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", "some exception message")
                );

            doThrow(exception).when(camundaServiceApi).addLocalVariablesToTask(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskId),
                any(AddLocalVariableRequest.class)
            );

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: "
                            + "Task complete failed. Unable to update task state to completed.");

        }

        @Test
        void completeTask_should_throw_a_task_complete_exception_when_completing_task_fails() {

            doThrow(mock(FeignException.class))
                .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: "
                            + "Task complete partially succeeded. "
                            + "The Task state was updated to completed, but the Task could not be completed.");

        }

    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class CompleteTaskWithPrivilegeAndCompletionOptions {
        UserInfo mockedUserInfo;
        String taskId;
        RoleAssignment mockedRoleAssignment;
        Map<String, CamundaVariable> mockedVariables;
        List<PermissionTypes> permissionsRequired;
        AccessControlResponse accessControlResponse;
        CompletionOptions mockedCompletionOptions;

        @BeforeEach
        void beforeEach() {
            taskId = UUID.randomUUID().toString();
            mockedRoleAssignment = mock(RoleAssignment.class);
            mockedVariables = mockVariables();
            mockedUserInfo = mock(UserInfo.class);
            mockedCompletionOptions = mock(CompletionOptions.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            permissionsRequired = asList(OWN, EXECUTE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void should_throw_exception_when_required_args_are_null() {

            assertThatThrownBy(() ->
                                   camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                       null,
                                       accessControlResponse,
                                       permissionsRequired,
                                       mockedCompletionOptions
                                   ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("TaskId cannot be null")
                .hasNoCause();

            assertThatThrownBy(() ->
                                   camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                       taskId,
                                       accessControlResponse,
                                       null,
                                       mockedCompletionOptions
                                   ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("PermissionsRequired cannot be null")
                .hasNoCause();

            when(mockedUserInfo.getUid()).thenReturn(null);

            assertThatThrownBy(() ->
                                   camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                       taskId,
                                       accessControlResponse,
                                       permissionsRequired,
                                       mockedCompletionOptions
                                   ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("UserId cannot be null")
                .hasNoCause();
        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            CamundaTask mockedCamundaTask;

            @BeforeEach
            void beforeEach() {
                when(mockedCompletionOptions.isAssignAndComplete()).thenReturn(false);
                mockedCamundaTask = createMockCamundaTask();
                when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedCamundaTask);

                when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                    mockedCamundaTask.getAssignee(),
                    IDAM_USER_ID,
                    mockedVariables,
                    singletonList(mockedRoleAssignment),
                    permissionsRequired
                )).thenReturn(true);

            }

            @Test
            void should_complete_task() {

                camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    permissionsRequired,
                    mockedCompletionOptions
                );

                Map<String, CamundaValue<String>> modifications = new HashMap<>();
                modifications.put("taskState", CamundaValue.stringValue("completed"));
                verify(camundaServiceApi).addLocalVariablesToTask(
                    BEARER_SERVICE_TOKEN,
                    taskId,
                    new AddLocalVariableRequest(modifications)
                );

                verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
                verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
            }


            @Test
            @MockitoSettings(strictness = Strictness.LENIENT)
            void should_fail_and_return_resource_not_found_exception_if_task_did_not_exist() {

                when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenThrow(FeignException.NotFound.class);

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage(String.format(
                        EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                        taskId
                    ));
            }

            @Test
            void should_not_call_task_state_update_if_task_state_already_complete() {
                mockedVariables.put("taskState", new CamundaVariable(COMPLETED.value(), "String"));

                camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    permissionsRequired,
                    mockedCompletionOptions
                );

                verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
            }

            @Test
            @MockitoSettings(strictness = Strictness.LENIENT)
            void should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

                TestFeignClientException exception =
                    new TestFeignClientException(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase()
                    );

                doThrow(exception)
                    .when(camundaServiceApi).getVariables(BEARER_SERVICE_TOKEN, taskId);

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage(String.format(
                        EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                        taskId
                    ));
            }

            @Test
            void should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

                when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                    mockedCamundaTask.getAssignee(),
                    IDAM_USER_ID,
                    mockedVariables,
                    singletonList(mockedRoleAssignment),
                    permissionsRequired
                )).thenReturn(false);


                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(RoleAssignmentVerificationException.class)
                    .hasNoCause()
                    .hasMessage("Role Assignment Verification: "
                                + "The request failed the Role Assignment checks performed.");
            }

            @Test
            void should_throw_a_task_complete_exception_when_addLocalVariablesToTask_fails() {

                TestFeignClientException exception =
                    new TestFeignClientException(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                        createCamundaTestException("aCamundaErrorType", "some exception message")
                    );

                doThrow(exception).when(camundaServiceApi).addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(TaskCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Complete Error: "
                                + "Task complete failed. Unable to update task state to completed.");

            }

            @Test
            void should_throw_a_task_completion_exception_when_completing_task_fails() {
                doThrow(mock(FeignException.class))
                    .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(TaskCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Complete Error: "
                                + "Task complete partially succeeded. "
                                + "The Task state was updated to completed, but the Task could not be completed.");

            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {
            @BeforeEach
            void beforeEach() {

                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    accessControlResponse.getRoleAssignments(),
                    permissionsRequired
                )).thenReturn(true);

                when(mockedCompletionOptions.isAssignAndComplete()).thenReturn(true);
            }

            @Test
            void should_complete_and_assign_task() {

                camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    permissionsRequired,
                    mockedCompletionOptions
                );

                Map<String, CamundaValue<String>> modifications = new HashMap<>();
                modifications.put("taskState", CamundaValue.stringValue("completed"));
                verify(camundaServiceApi).addLocalVariablesToTask(
                    BEARER_SERVICE_TOKEN,
                    taskId,
                    new AddLocalVariableRequest(modifications)
                );

                Map<String, String> assignBody = new ConcurrentHashMap<>();
                assignBody.put("userId", IDAM_USER_ID);

                verifyTaskStateUpdateWasCalled(taskId, TaskState.ASSIGNED);
                verify(camundaServiceApi).assignTask(BEARER_SERVICE_TOKEN, taskId, assignBody);
                verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
                verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
            }

            @Test
            void should_not_call_task_state_update_if_task_state_already_assigned() {

                mockedVariables.put("taskState", new CamundaVariable(ASSIGNED.value(), "String"));

                camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    permissionsRequired,
                    mockedCompletionOptions
                );

                Map<String, String> assignBody = new ConcurrentHashMap<>();
                assignBody.put("userId", IDAM_USER_ID);

                Map<String, CamundaValue<String>> modifications = Map.of(
                    CamundaVariableDefinition.TASK_STATE.value(), CamundaValue.stringValue(ASSIGNED.value())
                );

                verify(camundaServiceApi, times(0)).addLocalVariablesToTask(
                    BEARER_SERVICE_TOKEN,
                    taskId,
                    new AddLocalVariableRequest(modifications)
                );
                verify(camundaServiceApi).assignTask(BEARER_SERVICE_TOKEN, taskId, assignBody);
                verifyTaskStateUpdateWasCalled(taskId, TaskState.COMPLETED);
                verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
            }

            @Test
            @MockitoSettings(strictness = Strictness.LENIENT)
            void should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

                TestFeignClientException exception =
                    new TestFeignClientException(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase()
                    );

                doThrow(exception)
                    .when(camundaServiceApi).getVariables(BEARER_SERVICE_TOKEN, taskId);

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage(String.format(
                        EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                        taskId
                    ));
            }

            @Test
            void should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    singletonList(mockedRoleAssignment),
                    permissionsRequired
                )).thenReturn(false);


                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(RoleAssignmentVerificationException.class)
                    .hasNoCause()
                    .hasMessage("Role Assignment Verification: "
                                + "The request failed the Role Assignment checks performed.");
            }

            @Test
            void should_throw_a_task_assign_and_complete_exception_when_addLocalVariablesToTask_fails() {

                TestFeignClientException exception =
                    new TestFeignClientException(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                        createCamundaTestException("aCamundaErrorType", "some exception message")
                    );

                doThrow(exception).when(camundaServiceApi).addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(TaskAssignAndCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Assign and Complete Error: "
                                + "Task assign and complete partially succeeded. "
                                + "The Task was assigned to the user making the request but the "
                                + "Task could not be completed.");

            }

            @Test
            void should_throw_a_task_assign_and_complete_exception_when_completing_task_fails() {
                doThrow(mock(FeignException.class))
                    .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

                assertThatThrownBy(() ->
                                       camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                                           taskId,
                                           accessControlResponse,
                                           permissionsRequired,
                                           mockedCompletionOptions
                                       ))
                    .isInstanceOf(TaskAssignAndCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Assign and Complete Error: "
                                + "Task assign and complete partially succeeded. "
                                + "The Task was assigned to the user making the request, "
                                + "the task state was also updated to completed, but he Task could not be completed.");
            }
        }
    }

    @Nested
    @DisplayName("auto-complete()")
    @SuppressWarnings({"AbbreviationAsWordInName", "MemberNameCheck"})
    class AutoCompleteTask {

        private final String caseJurisdiction = "IA";
        private final String caseType = "Asylum";

        @Test
        void should_auto_complete_task() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                IDAM_USER_ID,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(singletonList(camundaTask));

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);


            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            when(mockedUserInfo.getUid()).thenReturn("dummyId");
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            mockCamundaGetAllVariables("someProcessInstanceId");

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);

            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );
            List<Task> tasks = response.getTasks();
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("IDAM_USER_ID", tasks.get(0).getAssignee());
            assertEquals("someTaskName", tasks.get(0).getName());
            assertEquals("someStaffLocationId", tasks.get(0).getLocation());
            assertEquals(false, tasks.get(0).getAutoAssigned());
            assertEquals("someStaffLocationName", tasks.get(0).getLocationName());
            assertEquals("00000", tasks.get(0).getCaseId());
            assertEquals("someCaseName", tasks.get(0).getCaseName());
            assertEquals("some_jurisdiction", tasks.get(0).getJurisdiction());
            assertEquals("someCaseType", tasks.get(0).getCaseTypeId());
            assertTrue(response.isTaskRequiredForEvent());
        }

        @Test
        void should_auto_complete_task_when_same_user() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                IDAM_USER_ID,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );
            CamundaTask camundaTask1 = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "DummyId",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(List.of(camundaTask, camundaTask1));

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);


            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            mockCamundaGetAllVariables("someProcessInstanceId");

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);

            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );
            List<Task> tasks = response.getTasks();

            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("IDAM_USER_ID", IDAM_USER_ID);
            assertEquals("someTaskName", tasks.get(0).getName());
            assertEquals("someStaffLocationId", tasks.get(0).getLocation());
            assertEquals(false, tasks.get(0).getAutoAssigned());
            assertEquals("someStaffLocationName", tasks.get(0).getLocationName());
            assertEquals("00000", tasks.get(0).getCaseId());
            assertEquals("someCaseName", tasks.get(0).getCaseName());
            assertEquals("some_jurisdiction", tasks.get(0).getJurisdiction());
            assertEquals("someCaseType", tasks.get(0).getCaseTypeId());
            assertTrue(response.isTaskRequiredForEvent());
        }

        @Test
        void should_auto_complete_and_return_empty_array_when_camunda_query_return_empty_results() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                IDAM_USER_ID,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );
            CamundaTask camundaTask1 = new CamundaTask(
                "someTaskId",
                "someTaskName",
                "DummyId",
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(emptyList());

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);

            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );
            List<Task> tasks = response.getTasks();
            assertTrue(tasks.isEmpty());
            assertFalse(response.isTaskRequiredForEvent());
        }

        @Test
        void should_auto_complete_task_when_has_permissions() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
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

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(singletonList(camundaTask));

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);


            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            mockCamundaGetAllVariables("someProcessInstanceId");

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            List<Task> tasks = response.getTasks();
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("someAssignee", tasks.get(0).getAssignee());
            assertEquals("someTaskName", tasks.get(0).getName());
            assertEquals("someStaffLocationId", tasks.get(0).getLocation());
            assertEquals(false, tasks.get(0).getAutoAssigned());
            assertEquals("someStaffLocationName", tasks.get(0).getLocationName());
            assertEquals("00000", tasks.get(0).getCaseId());
            assertEquals("someCaseName", tasks.get(0).getCaseName());
            assertTrue(response.isTaskRequiredForEvent());
        }

        @Test
        void should_auto_complete_and_return_empty_array__when_has_no_permissions() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
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

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(singletonList(camundaTask));

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);


            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            mockCamundaGetAllVariables("someProcessInstanceId");

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(false);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            List<Task> tasks = response.getTasks();
            assertTrue(tasks.isEmpty());
            assertFalse(response.isTaskRequiredForEvent());
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_dmn_evaluating_fails() {
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, PermissionTypes.MANAGE);
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "caseId", "eventId", caseJurisdiction, caseType);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo,
                singletonList(mockedRoleAssignment)
            );

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", "some exception message")
                );

            when(camundaServiceApi.evaluateDMN(eq(BEARER_SERVICE_TOKEN), any(), any()))
                .thenThrow(exception);


            assertThatThrownBy(() ->
                                   camundaService.searchForCompletableTasks(
                                       searchEventAndCase,
                                       permissionsRequired,
                                       accessControlResponse
                                   )
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem evaluating DMN")
                .hasCauseInstanceOf(FeignException.class);
        }

        @Test
        void should_auto_complete_and_return_empty_array() {

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);

            Map<String, CamundaVariable> body = new HashMap<>();
            body.put("eventId", new CamundaVariable(searchEventAndCase.getEventId(), "string"));

            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(new ArrayList<>());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            List<Task> tasks = response.getTasks();

            assertNotNull(tasks);
            assertEquals(0, tasks.size());
            assertFalse(response.isTaskRequiredForEvent());
        }

        @Test
        void searchWithCriteria_should_return_an_empty_task_list_when_camunda_takes_wrong_caseType() {
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, PermissionTypes.MANAGE);
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "caseId", "eventId", "caseJurisdiction", "caseType");

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo,
                singletonList(mockedRoleAssignment)
            );


            lenient().when(authTokenGenerator.generate()).thenReturn(String.valueOf(true));
            GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            assertTrue(response.getTasks().isEmpty());
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_search_call_fails() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someTaskId",
                "someTaskName",
                IDAM_USER_ID,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);

            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
                 )
            ).thenReturn(mockDMN());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            when(camundaServiceApi.searchWithCriteria(eq(BEARER_SERVICE_TOKEN), any())).thenThrow(FeignException.class);
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);

            assertThatThrownBy(
                () -> camundaService.searchForCompletableTasks(
                    searchEventAndCase,
                    permissionsRequired,
                    accessControlResponse
                )
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem performing the search")
                .hasCauseInstanceOf(FeignException.class);
        }

        @Test
        void should_complete_tasks_without_associated_task() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
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

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(singletonList(camundaTask));

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);


            String taskCompletionDmnKey = WA_TASK_COMPLETION.getTableKey(
                caseJurisdiction,
                caseType
            );

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(taskCompletionDmnKey),
                any()
            )).thenReturn(mockDMNWithEmptyRow());

            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            mockCamundaGetAllVariables("someProcessInstanceId");

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            final GetTasksCompletableResponse<Task> response = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            List<Task> tasks = response.getTasks();
            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("someAssignee", tasks.get(0).getAssignee());
            assertEquals("someTaskName", tasks.get(0).getName());
            assertEquals("someStaffLocationId", tasks.get(0).getLocation());
            assertEquals(false, tasks.get(0).getAutoAssigned());
            assertEquals("someStaffLocationName", tasks.get(0).getLocationName());
            assertEquals("00000", tasks.get(0).getCaseId());
            assertEquals("someCaseName", tasks.get(0).getCaseName());
            assertFalse(response.isTaskRequiredForEvent());
        }

        private void mockCamundaGetAllVariables(String someProcessInstanceId) {
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of(
                    "processInstanceIdIn", singletonList(someProcessInstanceId),
                    "processDefinitionKey", WA_TASK_INITIATION_BPMN_PROCESS_DEFINITION_KEY
                )
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId", "someTaskId"));
        }

    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {

        @Test
        void should_cancel_task() {
            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(CANCEL);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

            camundaService.cancelTask(taskId, accessControlResponse, permissionsRequired);

            verify(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());
        }

        @Test
        void cancelTask_should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(CANCEL);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception)
                .when(camundaServiceApi).getVariables(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            assertThatThrownBy(() -> camundaService.cancelTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));

        }

        @Test
        void cancelTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            List<PermissionTypes> permissionsRequired = asList(CANCEL);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(false);


            assertThatThrownBy(() -> camundaService.cancelTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
        }

        @Test
        void cancelTask_should_throw_a_task_cancel_exception_when_cancelling_task_fails() {

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(CANCEL);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());

            assertThatThrownBy(() -> camundaService.cancelTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");
        }

    }

    @Nested
    @DisplayName(("assignTask"))
    class AssignTask {

        @Test
        void should_succeed() {

            List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
            List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            final AccessControlResponse assigneeAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            final AccessControlResponse assignerAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                assignerPermissionsRequired
            )).thenReturn(true);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                assigneePermissionsRequired
            )).thenReturn(true);

            camundaService.assignTask(
                taskId, assignerAccessControlResponse,
                assignerPermissionsRequired, assigneeAccessControlResponse, assigneePermissionsRequired
            );

            verifyTaskStateUpdateWasCalled(taskId, TaskState.ASSIGNED);
        }

        @Test
        void assignTask_should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

            List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
            List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse assigneeAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            AccessControlResponse assignerAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception)
                .when(camundaServiceApi).getVariables(eq(BEARER_SERVICE_TOKEN), eq(taskId));

            assertThatThrownBy(
                () -> camundaService.assignTask(
                    taskId, assignerAccessControlResponse,
                    assignerPermissionsRequired, assigneeAccessControlResponse, assigneePermissionsRequired
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                    taskId
                ));
        }

        @Test
        void assignTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
            List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse assigneeAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            AccessControlResponse assignerAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                assignerPermissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(
                () -> camundaService.assignTask(
                    taskId, assignerAccessControlResponse,
                    assignerPermissionsRequired, assigneeAccessControlResponse, assigneePermissionsRequired
                ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");
        }

        @Test
        void assign_task_should_throw_task_assign_exception_exception_when_camunda_assign_task_endpoint_failed() {
            final List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
            final List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

            String taskId = UUID.randomUUID().toString();
            RoleAssignment mockedRoleAssignment = mock(RoleAssignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();
            mockedVariables.put("taskState", new CamundaVariable("assigned", "String"));

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse assigneeAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            AccessControlResponse assignerAccessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                assignerPermissionsRequired
            )).thenReturn(true);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                assigneePermissionsRequired
            )).thenReturn(true);

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception).when(camundaServiceApi).assignTask(any(), any(), any());
            assertThatThrownBy(
                () -> camundaService.assignTask(
                    taskId, assignerAccessControlResponse,
                    assignerPermissionsRequired, assigneeAccessControlResponse, assigneePermissionsRequired
                ))
                .isInstanceOf(TaskAssignException.class)
                .hasNoCause()
                .hasMessage("Task Assign Error: Task assign partially succeeded. "
                            + "The Task state was updated to assigned, but the Task could not be assigned.");
        }

    }

}
