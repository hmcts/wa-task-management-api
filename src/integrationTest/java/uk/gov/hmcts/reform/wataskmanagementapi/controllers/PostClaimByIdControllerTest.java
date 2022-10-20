package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostClaimByIdControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/claim";
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
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private AccessControlService accessControlService;
    @MockBean
    private IdamService idamService;

    private ServiceMocks mockServices;
    @Mock
    private UserInfo mockedUserInfo;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
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
                    .caseId("claimCaseId1")
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


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

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
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-assign-error"),
            jsonPath("$.title").value("Task Assign Error"),
            jsonPath("$.status").value(500),
            jsonPath("$.detail").value(
                "Task Assign Error: Task assign failed. Unable to update task state to assigned.")
        );
    }

    @Test
    void should_return_500_with_application_problem_response_when_claim_call_fails() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("claimCaseId1")
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


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        doNothing()
            .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
        FeignException mockedException = mock(FeignException.class);

        when(mockedException.contentUTF8()).thenReturn(
            "  {\n"
            + "    \"type\": \"Server Error\",\n"
            + "    \"message\": \"server error.\"\n"
            + "  }"
        );
        doThrow(mockedException).when(camundaServiceApi).assignTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is5xxServerError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-assign-error"),
            jsonPath("$.title").value("Task Assign Error"),
            jsonPath("$.status").value(500),
            jsonPath("$.detail").value(
                "Task Assign Error: Task assign partially succeeded. "
                + "The Task state was updated to assigned, but the Task could not be assigned.")
        );
    }

    @Test
    void should_return_500_with_application_problem_response_when_claim_call_fails_with_generic_exception()
        throws Exception {
        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("claimCaseId1")
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


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is5xxServerError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-assign-error"),
            jsonPath("$.title").value("Task Assign Error"),
            jsonPath("$.status").value(500),
            jsonPath("$.detail").value(
                "Task Assign Error: Task assign partially succeeded. "
                + "The Task state was updated to assigned, but the Task could not be assigned.")
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void should_return_a_403_when_the_user_jurisdiction_did_not_match(String jurisdiction, String caseType)
        throws Exception {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            "SCSS", "claimCaseId1");
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

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
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
    public void should_return_a_401_when_the_user_did_not_have_any_roles() throws Exception {
        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);
        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + "taskId")
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

        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(
            get("/task/" + nonExistentTaskId)
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
            get("/task/" + "taskId")
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

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(
            get("/task/" + nonExistentTaskId)
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

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_claim_task_when_grant_type_standard(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
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
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        verify(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
        verify(camundaServiceApi).assignTask(anyString(), anyString(), any());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_not_claim_task_when_grant_type_standard_and_excluded(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        //Excluded role
        taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
            true, true, true, true, true, true,
            new String[]{}, 1, false,
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
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

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_claim_task_when_grant_type_challenged(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_not_claim_task_when_grant_type_challenged_and_excluded(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        //Excluded role
        taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
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

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_claim_task_when_grant_type_specific(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void user_should_claim_task_when_grant_type_specific_and_excluded_(
        String jurisdiction, String caseType)
        throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_FTPA_JUDGE.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        //Excluded role
        taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("claimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }


    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("claimCaseId1");

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

}

