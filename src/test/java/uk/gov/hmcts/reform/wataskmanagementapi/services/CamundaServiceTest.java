package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaExceptionMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CompleteTaskVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions.TestFeignClientException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.InsufficientPermissionsException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.COMPLETED;

@ExtendWith(MockitoExtension.class)
class CamundaServiceTest {

    private static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    private static final String IDAM_USER_ID = "IDAM_USER_ID";

    @Mock
    AuthTokenGenerator authTokenGenerator;

    @Mock
    private CamundaServiceApi camundaServiceApi;

    @Mock
    private CamundaQueryBuilder camundaQueryBuilder;

    @Mock
    private PermissionEvaluatorService permissionEvaluatorService;

    private CamundaObjectMapper camundaObjectMapper;
    private CamundaService camundaService;

    @BeforeEach
    public void setUp() {
        camundaObjectMapper = new CamundaObjectMapper();
        CamundaErrorDecoder camundaErrorDecoder = new CamundaErrorDecoder();
        TaskMapper taskMapper = new TaskMapper(camundaObjectMapper);
        camundaService = new CamundaService(
            camundaServiceApi,
            camundaQueryBuilder,
            camundaErrorDecoder,
            taskMapper,
            authTokenGenerator,
            permissionEvaluatorService,
            camundaObjectMapper
        );

        when(authTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);
    }

    private Map<String, CamundaVariable> mockVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
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
            "someCamundaTaskFormKey"
        );
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

    private String createCamundaTestException(String type, String message) {
        return camundaObjectMapper.asCamundaJsonString(new CamundaExceptionMessage(type, message));
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
                    "There was a problem updating the task with id: %s. The task could not be found.",
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
        void searchWithCriteria_should_succeed() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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
                "someFormKey"
            );

            Map<String, CamundaVariable> variables = mockVariables();

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, camundaTask.getId()))
                .thenReturn(variables);

            when(permissionEvaluatorService.hasAccess(
                variables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(true);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                roleAssignment,
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
                .getVariables(BEARER_SERVICE_TOKEN, camundaTask.getId());
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_empty_list_if_user_did_not_have_sufficient_permission() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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
                "someFormKey"
            );

            Map<String, CamundaVariable> variables = mockVariables();

            when(camundaQueryBuilder.createQuery(searchTaskRequest))
                .thenReturn(camundaSearchQueryMock);
            when(camundaServiceApi.searchWithCriteria(BEARER_SERVICE_TOKEN, camundaSearchQueryMock.getQueries()))
                .thenReturn(singletonList(camundaTask));
            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, camundaTask.getId()))
                .thenReturn(variables);

            when(permissionEvaluatorService.hasAccess(
                variables,
                roleAssignment,
                permissionsRequired
            )).thenReturn(false);

            List<Task> results = camundaService.searchWithCriteria(
                searchTaskRequest,
                roleAssignment,
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
                .getVariables(BEARER_SERVICE_TOKEN, camundaTask.getId());
            verifyNoMoreInteractions(camundaServiceApi);
        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_local_variables_call_fails() {

            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
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

            when(camundaServiceApi.getVariables(eq(BEARER_SERVICE_TOKEN), any())).thenThrow(
                new TestFeignClientException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    createCamundaTestException("aCamundaErrorType", "some exception message")
                )
            );

            assertThatThrownBy(() ->
                camundaService.searchWithCriteria(searchTaskRequest, roleAssignment, permissionsRequired)
            )
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(ResourceNotFoundException.class)
                .hasMessage("There was a problem performing the search");


        }

        @Test
        void searchWithCriteria_should_throw_a_server_error_exception_when_camunda_search_call_fails() {
            List<Assignment> roleAssignment = singletonList(mock(Assignment.class));
            List<PermissionTypes> permissionsRequired = singletonList(READ);

            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQueryMock = mock(CamundaSearchQuery.class);


            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQueryMock);

            when(camundaServiceApi.searchWithCriteria(eq(BEARER_SERVICE_TOKEN), any())).thenThrow(FeignException.class);

            assertThatThrownBy(() ->
                camundaService.searchWithCriteria(searchTaskRequest, roleAssignment, permissionsRequired)
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
            verifyNoMoreInteractions(camundaServiceApi);
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
                    "There was a problem updating the task with id: %s. The task could not be found.",
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

            UserInfo mockedUserInfo = new UserInfo("email","someCamundaTaskAssignee",
                                                   new ArrayList<String>(),"name","givenName","familyName");

            List<PermissionTypes> permissionsRequired = asList(MANAGE);

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(mockedVariables);
            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(createMockCamundaTask());

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
        void unclaimTask_should_fail_as_different_user() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();
            UserInfo mockedUserInfo = new UserInfo("email","anot",
                                                   new ArrayList<String>(),"name","givenName","familyName");

            List<PermissionTypes> permissionsRequired = asList(MANAGE);
            AccessControlResponse accessControlResponse = new AccessControlResponse(
                mockedUserInfo, singletonList(mockedRoleAssignment)
            );

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(createMockCamundaTask());



            assertThatThrownBy(() -> camundaService.unclaimTask(taskId, accessControlResponse, permissionsRequired))
                .isInstanceOf(InsufficientPermissionsException.class)
                .hasMessage("Task was not claimed by this user");
        }

        @Test
        void unclaimTask_should_throw_resource_not_found_exception_when_other_exception_is_thrown() {

            String taskId = UUID.randomUUID().toString();
            String exceptionMessage = "some exception message";
            Assignment mockedRoleAssignment = mock(Assignment.class);

            UserInfo mockedUserInfo = new UserInfo("email","someCamundaTaskAssignee",
                                                   new ArrayList<String>(),"name","givenName","familyName");


            List<PermissionTypes> permissionsRequired = asList(MANAGE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(createMockCamundaTask());

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
                    "There was a problem updating the task with id: %s. The task could not be found.",
                    taskId
                ));
        }

        @Test
        void unclaimTask_should_throw_insufficient_permissions_exception_when_user_did_not_have_enough_permission() {

            String taskId = UUID.randomUUID().toString();
            Assignment mockedRoleAssignment = mock(Assignment.class);
            Map<String, CamundaVariable> mockedVariables = mockVariables();

            UserInfo mockedUserInfo = new UserInfo("email","someCamundaTaskAssignee",
                                                   new ArrayList<String>(),"name","givenName","familyName");


            List<PermissionTypes> permissionsRequired = asList(MANAGE);
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));

            when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, taskId)).thenReturn(createMockCamundaTask());
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
    }

    @Nested
    @DisplayName("assignTask()")
    @MockitoSettings(strictness = LENIENT)
    class AssignTask {
        private AccessControlResponse assignerAccessControlResponse;
        private AccessControlResponse assigneeAccessControlResponse;
        private String taskId;

        @BeforeEach
        void setUp() {
            assignerAccessControlResponse = mockAccessControl("some assigner user id");
            assigneeAccessControlResponse = mockAccessControl("some assignee user id");
            taskId = "some task id";

            when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, taskId)).thenReturn(Collections.emptyMap());
            when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(singletonList(MANAGE))))
                .thenReturn(true);
            when(permissionEvaluatorService.hasAccess(anyMap(), anyList(), eq(List.of(OWN, EXECUTE))))
                .thenReturn(true);
        }

        @Test
        void assignTask_should_succeed() {

            camundaService.assignTask(
                taskId,
                assignerAccessControlResponse,
                singletonList(MANAGE),
                assigneeAccessControlResponse,
                List.of(OWN, EXECUTE)
            );

            verify(camundaServiceApi, times(1))
                .addLocalVariablesToTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    any(AddLocalVariableRequest.class)
                );

            verify(camundaServiceApi, times(1))
                .assignTask(
                    eq(BEARER_SERVICE_TOKEN),
                    eq(taskId),
                    anyMap()
                );
        }

        @NotNull
        private AccessControlResponse mockAccessControl(String userId) {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            UserInfo userInfo = UserInfo.builder().uid(userId).build();

            when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
            when(accessControlResponse.getRoleAssignments())
                .thenReturn(Collections.singletonList(Assignment.builder().build()));
            return accessControlResponse;
        }

        @Test
        void assignTask_should_throw_server_error_exception_when_assignTask_fails() {

            TestFeignClientException exception =
                new TestFeignClientException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase()
                );

            doThrow(exception)
                .when(camundaServiceApi).assignTask(eq(BEARER_SERVICE_TOKEN), eq(taskId), anyMap());

            assertThatThrownBy(() -> camundaService.assignTask(
                taskId,
                assignerAccessControlResponse,
                singletonList(MANAGE),
                assigneeAccessControlResponse,
                List.of(OWN, EXECUTE)
            ))
                .isInstanceOf(ServerErrorException.class)
                .hasCauseInstanceOf(FeignException.class);

        }

        @Test
        void assignTask_should_throw_resource_not_found_exception_when_addLocalVariablesToTask_fails() {

            String exceptionMessage = "some exception message";

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

            assertThatThrownBy(() -> camundaService.assignTask(
                taskId,
                assignerAccessControlResponse,
                singletonList(MANAGE),
                assigneeAccessControlResponse,
                List.of(OWN, EXECUTE)
            ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasCauseInstanceOf(FeignException.class)
                .hasMessage(
                    String.format(
                        "There was a problem updating the task with id: %s. The task could not be found.", taskId
                    )
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
            verifyNoMoreInteractions(camundaServiceApi);
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
                    "There was a problem updating the task with id: %s. The task could not be found.",
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


}
