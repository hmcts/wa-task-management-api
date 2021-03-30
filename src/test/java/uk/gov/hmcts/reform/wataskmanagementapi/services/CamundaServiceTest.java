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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService.WA_TASK_COMPLETION_TABLE_NAME;


class CamundaServiceTest extends CamundaServiceBaseTest {

    public static final String EXPECTED_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK =
        "There was a problem fetching the variables for task with id: %s";

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

    private CamundaTask createMockCamundaTask() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            "someCamundaTaskAssignee",
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    private List<Map<String, CamundaVariable>> mockDMN() {

        //A List (Array) with a map (One object) with objects inside the object (String and CamundaVariable).
        List<Map<String, CamundaVariable>> array = new ArrayList<>();
        Map<String, CamundaVariable> dmnResult = new HashMap<>();
        dmnResult.put("ccdId", new CamundaVariable("00000", "String"));
        dmnResult.put("caseName", new CamundaVariable("someCaseName", "String"));
        array.add(dmnResult);
        return array;
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

    private List<CamundaVariableInstance> mockedVariablesResponse(String processInstanceId) {
        Map<String, CamundaVariable> mockVariables = mockVariables();

        return mockVariables.keySet().stream()
            .map(
                mockVarKey ->
                    new CamundaVariableInstance(
                        mockVariables.get(mockVarKey).getValue(),
                        mockVariables.get(mockVarKey).getType(),
                        mockVarKey,
                        processInstanceId
                    ))
            .collect(Collectors.toList());

    }

    private List<CamundaVariableInstance> mockedVariablesResponseForMultipleProcessIds() {
        List<CamundaVariableInstance> variablesForProcessInstance1 = mockedVariablesResponse("someProcessInstanceId");
        List<CamundaVariableInstance> variablesForProcessInstance2 = mockedVariablesResponse("someProcessInstanceId2");

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
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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
            assertEquals("someCamundaTaskAssignee", response.getAssignee());

        }


        @Test
        void getTask_should_throw_insufficient_permissions_exception_when_has_access_returns_false() {

            String taskId = UUID.randomUUID().toString();
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(() -> camundaService.getTask(taskId, roleAssignment, permissionsRequired))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("User did not have sufficient permissions to access task with id: " + taskId);

        }

        @Test
        void getTask_should_throw_a_server_error_exception_exception_when_feign_exception_is_thrown_by_get_variables() {

            String taskId = UUID.randomUUID().toString();
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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
                .hasCauseInstanceOf(FeignException.class);

        }
    }

    @Nested
    @DisplayName("searchWithCriteria()")
    class SearchWithCriteria {

        @Test
        void given_task_assigneeId_equals_to_userId_then_add_task_once() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            UserInfo userInfoMock = mock(UserInfo.class);
            String someAssignee = "someAssignee";
            when(userInfoMock.getUid()).thenReturn(someAssignee);
            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfoMock, roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
            verifyNoMoreInteractions(permissionEvaluatorService);
        }

        @Test
        void searchWithCriteria_should_succeed() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_query_was_null() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(null);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));


            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }


        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        void searchWithCriteria_should_succeed_and_return_empty_list_if_task_did_not_have_variables() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("anotherProcessInstanceId"));

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_when_multiple_tasks_returned_and_sufficient_permission() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
                "someId2",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_when_multiple_tasks_returned_and_only_one_with_variables_sufficient_permission() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
                "someId2",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_succeed_and_return_empty_list_when_multiple_tasks_returned_and_not_sufficient_permission() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
                "someId2",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(asList(camundaTask, camundaTask2));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", asList("someProcessInstanceId", "someProcessInstanceId2"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_when_multiple_process_variables_returned_and_sufficient_permission() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
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
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void should_return_empty_list_when_multiple_process_variables_returned_and_user_did_not_have_permissions() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);

            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponseForMultipleProcessIds());

            when(permissionEvaluatorService.hasAccess(
                anyMap(),
                eq(roleAssignment),
                eq(permissionsRequired)
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_local_variables_call_fails() {

            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(mock(CamundaTask.class)));

            when(camundaServiceApi.searchWithCriteria(
                BEARER_SERVICE_TOKEN,
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
                camundaService.searchWithCriteria(searchTaskRequest, accessControlResponse, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(TestFeignClientException.class)
                .hasMessage("There was a problem performing the search");
        }


        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_searchWithCriteria_returns_emptyList() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);

            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(emptyList());

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_camunda_getAllVariables_returns_emptyList() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(emptyList());

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(results);
            assertEquals(0, results.size());
            verify(camundaQueryBuilder, times(1)).createQuery(searchTaskRequest);
            verifyNoMoreInteractions(camundaQueryBuilder);
            verify(camundaServiceApi, times(1)).searchWithCriteria(
                BEARER_SERVICE_TOKEN,
                camundaSearchQueryMock.getQueries()
            );
            verify(camundaServiceApi, times(1))
                .getAllVariables(
                    BEARER_SERVICE_TOKEN,
                    Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
                );
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_search_call_fails() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mock(UserInfo.class), roleAssignment);
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);


            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);

            when(camundaServiceApi.searchWithCriteria(eq(BEARER_SERVICE_TOKEN), any())).thenThrow(FeignException.class);

            assertThatThrownBy(() ->
                camundaService.searchWithCriteria(searchTaskRequest, accessControlResponse, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasMessage("There was a problem performing the search")
                .hasCauseInstanceOf(FeignException.class);
        }

    }

    @Nested
    @DisplayName("claimTask()")
    class ClaimTask {
        @Test
        void claimTask_should_succeed() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
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
            Assignment mockedRoleAssignment = mock(Assignment.class);

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
            Assignment mockedRoleAssignment = mock(Assignment.class);
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
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasNoCause()
                .hasMessage(
                    String.format("User did not have sufficient permissions to claim task with id: %s", taskId));
        }

        @Test
        void claimTask_should_throw_exception_when_updateTaskState_failed() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
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

            assertThatThrownBy(() -> camundaService.claimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage("some exception message");
        }
    }

    @Nested
    @DisplayName("unclaimTask()")
    class UnclaimTask {
        @Test
        void unclaimTask_should_succeed() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = new UserInfo("email", "someCamundaTaskAssignee",
                new ArrayList<String>(), "name", "givenName", "familyName");

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
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
            Assignment mockedRoleAssignment = mock(Assignment.class);

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
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(UserInfo.builder().build(), singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(false);

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasNoCause()
                .hasMessage(String.format(
                    "User did not have sufficient permissions to unclaim task with id: %s",
                    taskId)
                );
        }

        @Test
        void unclaimTask_should_throw_a_server_error_exception_when_unclaim_task_fails() {
            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            List<PermissionTypes> permissionsRequired = singletonList(MANAGE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                singletonList(mockedRoleAssignment),
                permissionsRequired
            )).thenReturn(true);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                UserInfo.builder().build(), singletonList(mockedRoleAssignment)
            );

            doThrow(FeignException.class)
                .when(camundaServiceApi).unclaimTask(any(), any());

            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    "There was a problem unclaiming task: %s",
                    taskId)
                );
        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {

        @Test
        void should_complete_task() {
            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

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
        void completeTask_does_not_call_camunda_task_state_update_complete_if_task_already_complete() {
            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();
            mockedVariables.put("taskState", new CamundaVariable(COMPLETED.value(), "String"));

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

            camundaService.completeTask(taskId, accessControlResponse, permissionsRequired);

            verify(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());
        }

        @Test
        void completeTask_should_throw_resource_not_found_exception_when_getVariables_threw_exception() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);

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

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
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


            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasNoCause()
                .hasMessage(
                    String.format("User did not have sufficient permissions to complete task with id: %s", taskId));
        }

        @Test
        void completeTask_should_throw_a_server_error_exception_when_addLocalVariablesToTask_fails() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

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
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    "There was a problem completing the task with id: %s",
                    taskId));

        }

        @Test
        void completeTask_should_throw_a_server_error_exception_when_completing_task_fails() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = mock(UserInfo.class);
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = singletonList(READ);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);

            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            )).thenReturn(true);

            doThrow(mock(FeignException.class))
                .when(camundaServiceApi).completeTask(BEARER_SERVICE_TOKEN, taskId, new CompleteTaskVariables());

            assertThatThrownBy(() -> camundaService.completeTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    "There was a problem completing the task with id: %s",
                    taskId));

        }

    }

    @Nested
    @DisplayName("auto-complete()")
    @SuppressWarnings({"AbbreviationAsWordInName", "MemberNameCheck"})
    class AutoCompleteTask {

        private final String caseJurisdiction = "IA";
        private final String caseType = "Asylum";

        @Test
        void should_auto_complete_task_when_same_user() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
                "someTaskName",
                IDAM_USER_ID,
                ZonedDateTime.now(),
                dueDate,
                null,
                null,
                "someFormKey",
                "someProcessInstanceId"
            );

            Map<String, CamundaVariable> variables = mockVariables();

            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQuery.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);


            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);

            when(camundaServiceApi.evaluateDMN(eq(BEARER_SERVICE_TOKEN),
                eq(getTableKey(caseJurisdiction, caseType)),
                any())
            ).thenReturn(mockDMN());

            Assignment mockedRoleAssignment = mock(Assignment.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            List<Task> tasks = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );


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
        }

        @Test
        void should_auto_complete_task_when_has_permissions() {
            ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            CamundaTask camundaTask = new CamundaTask(
                "someId",
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
            when(mockedUserInfo.getUid()).thenReturn(IDAM_USER_ID);

            SearchEventAndCase searchEventAndCase = mock(SearchEventAndCase.class);
            when(searchEventAndCase.getCaseId()).thenReturn("caseId");
            when(searchEventAndCase.getEventId()).thenReturn("eventId");
            when(searchEventAndCase.getCaseJurisdiction()).thenReturn(caseJurisdiction);
            when(searchEventAndCase.getCaseType()).thenReturn(caseType);

            when(camundaServiceApi.evaluateDMN(
                eq(BEARER_SERVICE_TOKEN),
                eq(getTableKey(caseJurisdiction, caseType)),
                any())
            ).thenReturn(mockDMN());

            Assignment mockedRoleAssignment = mock(Assignment.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaQueryBuilder.createCompletableTasksQuery(eq(searchEventAndCase.getCaseId()), any()))
                .thenReturn(camundaSearchQuery);

            when(camundaServiceApi.getAllVariables(
                BEARER_SERVICE_TOKEN,
                Map.of("variableScopeIdIn", singletonList("someProcessInstanceId"))
            )).thenReturn(mockedVariablesResponse("someProcessInstanceId"));

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            when(permissionEvaluatorService.hasAccess(
                any(),
                eq(accessControlResponse.getRoleAssignments()),
                eq(permissionsRequired)
            )).thenReturn(true);

            List<Task> tasks = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );

            assertNotNull(tasks);
            assertEquals(1, tasks.size());
            assertEquals("someAssignee", tasks.get(0).getAssignee());
            assertEquals("someTaskName", tasks.get(0).getName());
            assertEquals("someStaffLocationId", tasks.get(0).getLocation());
            assertEquals(false, tasks.get(0).getAutoAssigned());
            assertEquals("someStaffLocationName", tasks.get(0).getLocationName());
            assertEquals("00000", tasks.get(0).getCaseId());
            assertEquals("someCaseName", tasks.get(0).getCaseName());

        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_dmn_evaluating_fails() {
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, PermissionTypes.MANAGE);
            Assignment mockedRoleAssignment = mock(Assignment.class);
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

            when(camundaServiceApi.evaluateDMN(eq(BEARER_SERVICE_TOKEN), eq(getTableKey(caseJurisdiction, caseType)),
                any()
            ))
                .thenReturn(new ArrayList<>());

            Assignment mockedRoleAssignment = mock(Assignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, EXECUTE);
            List<Task> tasks = camundaService.searchForCompletableTasks(
                searchEventAndCase,
                permissionsRequired,
                accessControlResponse
            );


            assertNotNull(tasks);
            assertEquals(0, tasks.size());
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_takes_wrong_caseType() {
            List<PermissionTypes> permissionsRequired = asList(PermissionTypes.OWN, PermissionTypes.MANAGE);
            Assignment mockedRoleAssignment = mock(Assignment.class);
            UserInfo mockedUserInfo = mock(UserInfo.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "caseId", "eventId", "caseJurisdiction", "caseType");

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo,
                singletonList(mockedRoleAssignment));


            lenient().when(authTokenGenerator.generate()).thenReturn(String.valueOf(true));

            assertThatThrownBy(() ->
                camundaService.searchForCompletableTasks(
                    searchEventAndCase,
                    permissionsRequired,
                    accessControlResponse)
            )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Please check your request. This endpoint "
                            + "currently only supports the Immigration & "
                            + "Asylum service");
        }


        private String getTableKey(String jurisdictionId, String caseTypeId) {
            return WA_TASK_COMPLETION_TABLE_NAME + "-" + jurisdictionId.toLowerCase(Locale.getDefault())
                   + "-" + caseTypeId.toLowerCase(Locale.getDefault());
        }
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {

        @Test
        void should_cancel_task() {
            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
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
            Assignment mockedRoleAssignment = mock(Assignment.class);

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
            Assignment mockedRoleAssignment = mock(Assignment.class);
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
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasNoCause()
                .hasMessage(
                    String.format("User did not have sufficient permissions to cancel task with id: %s", taskId));
        }

        @Test
        void cancelTask_should_throw_a_server_error_exception_when_cancelling_task_fails() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
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

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", String.format(
                        "There was a problem cancelling the task with id: %s",
                        taskId))
                );

            doThrow(exception)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), anyMap());

            assertThatThrownBy(() -> camundaService.cancelTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(String.format(
                    "There was a problem cancelling the task with id: %s",
                    taskId));

        }

    }

}
