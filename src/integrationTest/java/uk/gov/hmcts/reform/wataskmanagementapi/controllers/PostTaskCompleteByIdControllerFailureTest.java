package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
class PostTaskCompleteByIdControllerFailureTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/complete";
    private static String ENDPOINT_BEING_TESTED;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private AccessControlService accessControlService;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private RoleAssignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    private ServiceMocks mockServices;
    private String taskId;
    @MockBean
    CftQueryService cftQueryService;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        lenient().when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        lenient().when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        lenient().when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);
        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        initiateATask(taskId);
    }

    private void initiateATask(String id) {

        TaskResource taskResource = new TaskResource(
            id,
            "taskName",
            "taskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
    }

    @Nested
    @DisplayName("with privileged access")
    class CompleteTaskWithPrivilegedAccess {

        @BeforeEach
        void beforeEach() {
            mockServices.mockServiceAPIs();
            when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
                .thenReturn(true);

        }

        @Test
        void should_return_500_with_application_problem_response_when_task_update_call_fails() throws Exception {
            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                    jsonPath("$.title").value("Task Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Complete Error: Task complete failed. Unable to update task state to completed.")
                ));
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails() throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).completeTask(
                any(),
                any(),
                any()
            );

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                    jsonPath("$.title").value("Task Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Complete Error: Task complete partially succeeded. "
                        + "The Task state was updated to completed, but the Task could not be completed.")
                ));
        }

        @Test
        void should_return_500_with_application_problem_response_when_task_update_call_fails_with_completion_options()
            throws Exception {

            when(permissionEvaluatorService.hasAccess(any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"),
                    jsonPath("$.title").value("Task Assign and Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                        + "The Task was assigned to the user making the request but the Task could not be completed.")
                ));
        }

        @Test
        void should_return_500_with_application_problem_response_when_assign_call_fails_with_completion_options()
            throws Exception {

            when(permissionEvaluatorService.hasAccess(any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"),
                    jsonPath("$.title").value("Task Assign and Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Assign and Complete Error: Unable to assign the Task to the current user.")
                ));
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails_with_completion_options()
            throws Exception {

            when(permissionEvaluatorService.hasAccess(any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).completeTask(
                any(),
                any(),
                any()
            );

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"),
                    jsonPath("$.title").value("Task Assign and Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                        + "The Task was assigned to the user making the request, "
                        + "the task state was also updated to completed, but he Task could not be completed.")
                ));
        }

        @Test
        void should_return_403_with_application_problem_response_when_completion_options_value_is_null()
            throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(false);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(asJsonString(new CompleteTaskRequest(null)))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    ResultMatcher.matchAll(
                        status().isForbidden(),
                        content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                        jsonPath("$.type").value(
                            "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                        jsonPath("$.title").value("Role Assignment Verification"),
                        jsonPath("$.status").value(403),
                        jsonPath("$.detail").value(
                            "Role Assignment Verification: "
                            + "The request failed the Role Assignment checks performed.")
                    ));
        }

        @Test
        void should_return_400_bad_request_application_problem_when_completion_options_value_is_null()
            throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doNothing().when(camundaServiceApi).completeTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\n"
                                 + "  \"completionOptions\": {\n"
                                 + "    \"assignAndComplete\": null\n"
                                 + "  }\n"
                                 + "}")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    ResultMatcher.matchAll(
                        status().isBadRequest()
                    ));

        }

        @Test
        void should_return_400_bad_request_application_problem_when_unknown_property_provided_in_completion_options()
            throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doNothing().when(camundaServiceApi).completeTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\n"
                                 + "  \"completionOptions\": {\n"
                                 + "    \"anotherNonExistingProperty\": true\n"
                                 + "  }\n"
                                 + "}")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    ResultMatcher.matchAll(
                        status().isBadRequest()
                    ));
        }

        @Test
        void should_return_400_bad_request_application_problem_when_completion_options_invalid_value()
            throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doNothing().when(camundaServiceApi).completeTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\n"
                                 + "  \"completionOptions\": {\n"
                                 + "    \"assignAndComplete\": \"stringValue\"\n"
                                 + "  }\n"
                                 + "}")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    ResultMatcher.matchAll(
                        status().isBadRequest()
                    ));
        }

        @Test
        public void should_return_a_403_when_the_user_did_not_have_correct_jurisdiction() throws Exception {

            CFTTaskDatabaseService cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);
            insertDummyTaskInDb(taskId, cftTaskDatabaseService);

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "SCSS", "caseId1");
            // create role assignments Organisation and SCSS , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo,
                                                      roleAssignmentsWithJurisdiction));

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                IDAM_USER_ID,
                IDAM_USER_EMAIL
            )).thenReturn(true);

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(request))
            ).andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                jsonPath("$.title").value("Role Assignment Verification"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Role Assignment Verification: "
                    + "The request failed the Role Assignment checks performed.")
            );
        }
    }



    @Nested
    @DisplayName("with no privileged access")
    class CompleteTaskWithNoPrivilegedAccess {

        @BeforeEach
        void beforeEach() {
            when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
                .thenReturn(false);
        }

        @Test
        void should_return_500_with_application_problem_response_when_task_update_call_fails() throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                    jsonPath("$.title").value("Task Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Complete Error: Task complete failed. Unable to update task state to completed.")
                ));
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails() throws Exception {

            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(any(), any(), any(), any(), any()))
                .thenReturn(true);

            CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
            when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).completeTask(
                any(),
                any(),
                any()
            );

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(
                ResultMatcher.matchAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                    jsonPath("$.title").value("Task Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Complete Error: Task complete partially succeeded. "
                        + "The Task state was updated to completed, but the Task could not be completed.")
                ));
        }

        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_privileged_and_completion_options()
            throws Exception {

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(
                    ResultMatcher.matchAll(
                        status().isForbidden(),
                        content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                        jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                        jsonPath("$.title").value("Forbidden"),
                        jsonPath("$.status").value(403),
                        jsonPath("$.detail").value(
                            "Forbidden: "
                            + "The action could not be completed because the client/user had insufficient rights to a resource.")
                    ));
        }
    }

    private void insertDummyTaskInDb(String taskId, CFTTaskDatabaseService cftTaskDatabaseService) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction("IA");
        taskResource.setCaseTypeId("Asylum");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("caseId1");


        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, true, true, false, false,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        tribunalResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }
}

