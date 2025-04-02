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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
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
    private IdamService idamService;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private UserInfo mockedUserInfo;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    private ServiceMocks mockServices;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        lenient().when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        lenient().when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Nested
    @DisplayName("with privileged access")
    class CompleteTaskWithPrivilegedAccess {

        @BeforeEach
        void beforeEach() {
            when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
                .thenReturn(true);

        }

        @Test
        void should_return_500_with_application_problem_response_when_task_update_call_fails() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpectAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                jsonPath("$.title").value("Task Complete Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Complete Error: Task complete failed. Unable to update task state to completed.")
            );
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

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
            ).andExpectAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                jsonPath("$.title").value("Task Complete Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Complete Error: Task complete partially succeeded. "
                    + "The Task state was updated to completed, but the Task could not be completed.")
            );
        }

        @Test
        void should_return_500_with_application_problem_response_when_task_update_call_fails_with_completion_options()
            throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);


            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
            ).andExpectAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"),
                jsonPath("$.title").value("Task Assign and Complete Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                    + "The Task was assigned to the user making the request but the Task could not be completed.")
            );
        }

        @Test
        void should_return_500_with_application_problem_response_when_assign_call_fails_with_completion_options()
            throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
            ).andExpectAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-assign-and-complete-error"),
                jsonPath("$.title").value("Task Assign and Complete Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Assign and Complete Error: Unable to assign the Task to the current user.")
            );
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails_with_completion_options()
            throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
            ).andExpectAll(
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
            );
        }

        @Test
        void should_return_403_with_application_problem_response_when_completion_options_value_is_null()
            throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                true, false, false, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
                        .content(asJsonString(new CompleteTaskRequest(null)))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpectAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                    jsonPath("$.title").value("Role Assignment Verification"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Role Assignment Verification: "
                        + "The request failed the Role Assignment checks performed.")
                );
        }

        @Test
        void should_return_400_bad_request_application_problem_when_completion_options_value_is_null()
            throws Exception {

            mockServices.mockUserInfo();

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
                .andExpectAll(
                    status().isBadRequest()
                );

        }

        @Test
        void should_return_400_bad_request_application_problem_when_unknown_property_provided_in_completion_options()
            throws Exception {

            mockServices.mockUserInfo();

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
                .andExpectAll(
                    status().isBadRequest()
                );
        }

        @Test
        void should_return_400_bad_request_application_problem_when_completion_options_invalid_value()
            throws Exception {

            mockServices.mockUserInfo();

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
                .andExpectAll(
                    status().isBadRequest()
                );
        }

        @Test
        public void should_return_a_403_when_the_user_did_not_have_correct_jurisdiction() throws Exception {
            mockServices.mockUserInfo();
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "SCSS", "completeFailureCaseId1");
            // create role assignments Organisation and SCSS , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

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

        @Test
        public void should_return_a_404_if_task_does_not_exist() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "SCSS", "caseId1");
            // create role assignments Organisation and SCSS , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

            mockMvc.perform(
                post(String.format(ENDPOINT_PATH, nonExistentTaskId))
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(request))
            ).andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
                jsonPath("$.title").value("Task Not Found Error"),
                jsonPath("$.status").value(404),
                jsonPath("$.detail").value(
                    "Task Not Found Error: The task could not be found.")
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

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpectAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                jsonPath("$.title").value("Task Complete Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Complete Error: Task complete failed. Unable to update task state to completed.")
            );
        }

        @Test
        void should_return_500_with_application_problem_response_when_complete_call_fails() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
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
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpectAll(
                    status().is5xxServerError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value(
                        "https://github.com/hmcts/wa-task-management-api/problem/task-complete-error"),
                    jsonPath("$.title").value("Task Complete Error"),
                    jsonPath("$.status").value(500),
                    jsonPath("$.detail").value(
                        "Task Complete Error: Task complete partially succeeded. "
                        + "The Task state was updated to completed, but the Task could not be completed.")
                );
        }

        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_privileged_and_completion_options()
            throws Exception {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeFailureCaseId1")
                        .build()
                )
                .build();

            createRoleAssignment(roleAssignments, roleAssignmentRequest);
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(asJsonString(new CompleteTaskRequest(new CompletionOptions(true))))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpectAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                    jsonPath("$.title").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Forbidden: "
                        + "The action could not be completed because the client/user had insufficient rights to a resource.")
                );
        }

        @Test
        public void should_return_a_404_if_task_does_not_exist() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "SCSS", "caseId1");
            // create role assignments Organisation and SCSS , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";
            mockMvc.perform(
                    post(String.format(ENDPOINT_PATH, nonExistentTaskId))
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                )
                .andExpectAll(
                    status().is4xxClientError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
                    jsonPath("$.title").value("Task Not Found Error"),
                    jsonPath("$.status").value(404),
                    jsonPath("$.detail").value(
                        "Task Not Found Error: The task could not be found.")
                );
        }

        @Test
        public void should_return_a_401_when_the_user_did_not_have_any_roles() throws Exception {
            List<RoleAssignment> roles = new ArrayList<>();

            RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);
            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            //Assigner
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(roleAssignmentResource);

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.error").value("Unauthorized"),
                jsonPath("$.status").value(401),
                jsonPath("$.message").value(
                    "User did not have sufficient permissions to perform this action"));
        }
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId, TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            ASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("completeFailureCaseId1");
        taskResource.setAssignee(IDAM_USER_ID);
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }
}

