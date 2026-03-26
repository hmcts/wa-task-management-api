package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController.REQ_PARAM_COMPLETION_PROCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_OTHER_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID_GP;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
class PostTaskCompleteByIdControllerTest {

    private static final String ENDPOINT_PATH = "/task/%s/complete";
    private static String ENDPOINT_BEING_TESTED;
    @Autowired
    private IdamWebApi idamWebApi;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockitoBean
    private IdamService idamService;
    @Mock
    private UserInfo mockedUserInfo;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;
    private ServiceMocks mockServices;
    private String taskId;
    @MockitoBean
    private AccessControlService accessControlService;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    IntegrationTestUtils integrationTestUtils;
    RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

    @MockitoBean
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @BeforeEach
    void beforeEach() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_COMPLETION_PROCESS_UPDATE),
                                                                       anyString(), anyString())).thenReturn(false);

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        lenient().when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        lenient().when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);

        when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
            .thenReturn(false);
    }

    @Test
    void should_not_update_cft_db_when_camunda_complete_api_fails() throws Exception {
        mockServices.mockUserInfo();

        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("completeCaseId1")
                    .build()
            )
            .build();
        when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
            .thenReturn(true);
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any()))
            .thenReturn(accessControlResponse);
        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).addLocalVariablesToTask(anyString(), anyString(), any());

        doThrow(feignException(502))
            .when(camundaServiceApi)
            .completeTask(anyString(), eq(taskId), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
        ).andExpect(status().is5xxServerError());

        verify(camundaServiceApi, times(3)).completeTask(anyString(), eq(taskId), any());

        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);
        assertThat(taskResource).isPresent();
        assertThat(taskResource.get().getState()).isNotEqualTo(COMPLETED);
        assertThat(taskResource.get().getState()).isEqualTo(ASSIGNED);
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
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_standard() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(integrationTestUtils.asJsonString(request))
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @Test
        void should_not_succeed_and_return_403_and_update_cft_task_state_with_grant_type_standard_and_excluded() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            //Excluded role
            taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(integrationTestUtils.asJsonString(request))
                )
                .andExpectAll(
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
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_challenged() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(integrationTestUtils.asJsonString(request))
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @Test
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_specific() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(integrationTestUtils.asJsonString(request))
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }


        @Test
        void should_return_a_403_when_user_jurisdiction_did_not_match_and_assign_and_complete_true() throws Exception {

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "WA", "completeCaseId1");
            // create role assignments Organisation and WA , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            List<RoleAssignment> roleAssignments = new ArrayList<>();
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(request))
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
        void should_return_a_204_when_the_user_ids_are_different() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            lenient().when(mockedUserInfo.getUid())
                .thenReturn(IDAM_OTHER_USER_ID);
            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(request))
            ).andExpectAll(
                status().isNoContent()
            );

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_OTHER_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

    }

    @Nested
    @DisplayName("with no privileged access")
    class CompleteTaskWithNoPrivilegedAccess {

        @Test
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_standard() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @ParameterizedTest
        @CsvSource(value = {
            "EXUI_USER_COMPLETION",
            "EXUI_CASE-EVENT_COMPLETION",
            "NULL",
            "''"
        }, nullValues = "NULL")
        void should_succeed_and_return_204_and_update_completion_process_when_flag_enabled(
            String completionProcess) throws Exception {
            mockServices.mockUserInfo();

            lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_COMPLETION_PROCESS_UPDATE),
                                                                           anyString(), anyString())).thenReturn(true);

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .param(REQ_PARAM_COMPLETION_PROCESS, completionProcess)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
            if (completionProcess == null || completionProcess.isBlank()) {
                assertNull(taskResource.get().getTerminationProcess());
            } else {
                assertEquals(TerminationProcess.fromValue(completionProcess).get(),
                             taskResource.get().getTerminationProcess());
            }
        }

        @ParameterizedTest
        @CsvSource(value = {
            "EXUI_USER_COMPLETION",
            "EXUI_CASE-EVENT_COMPLETION"
        })
        void should_succeed_and_return_204_and_not_update_completion_process_when_flag_disabled(
            String completionProcess) throws Exception {
            mockServices.mockUserInfo();

            lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_COMPLETION_PROCESS_UPDATE),
                                                                           anyString(), anyString())).thenReturn(false);

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .param(REQ_PARAM_COMPLETION_PROCESS, completionProcess)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
            assertNull(taskResource.get().getTerminationProcess());
        }

        @Test
        void should_not_succeed_and_return_403_and_update_cft_task_state_with_grant_type_standard_and_excluded()
            throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            //Excluded role
            taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpectAll(
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
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_challenged() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @Test
        void should_succeed_and_return_204_and_update_cft_task_state_with_grant_type_specific() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @Test
        void should_return_a_204_when_task_is_already_completed() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(IDAM_USER_ID, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @Test
        void should_return_a_403_if_task_was_not_previously_assigned() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
            );
            insertUnassignTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpectAll(
                    status().is4xxClientError(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.error").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.message").value(
                        "Could not complete task with id: " + taskId + " as task was not previously assigned")
                );

        }


        @Test
        void should_return_a_403_when_the_user_did_not_have_sufficient_jurisdiction_did_not_match() throws Exception {

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, true, true, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
                taskId, OffsetDateTime.now(),
                false, false, false, false, false, false,
                false, false, false, false
            );
            insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
                "WA", "completeCaseId1");
            // create role assignments Organisation and WA , Case Id
            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
                roleAssignmentsWithJurisdiction
            );

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);
            List<RoleAssignment> roleAssignments = new ArrayList<>();
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
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
        void should_return_a_403_when_the_user_ids_are_different() throws Exception {
            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            lenient().when(mockedUserInfo.getUid())
                .thenReturn(IDAM_OTHER_USER_ID);
            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
            ).andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_JSON_VALUE),
                jsonPath("$.message").value("Could not complete task with id: " + taskId + " as task was assigned to other user IDAM_USER_ID")
            );
        }

        @Test
        void should_return_403_when_no_privileged_acccess_and_assign_and_complete_true() throws Exception {

            mockServices.mockUserInfo();
            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("IA")
                        .caseType("Asylum")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

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
            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
            doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                    post(ENDPOINT_BEING_TESTED)
                        .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                        .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(integrationTestUtils.asJsonString(request))
                )
                .andExpectAll(
                    status().is4xxClientError(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                    jsonPath("$.title").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Forbidden: The action could not be completed because the client/user "
                        + "had insufficient rights to a resource.")
                );
        }

    }

    @Nested
    @DisplayName("without privileged access")
    class CompleteTaskWithoutPrivilegedAccess {

        @ParameterizedTest
        @CsvSource(value = {
            "true, false, false, false, IDAM_USER_ID_GP",
            "false, true, false, false, IDAM_USER_ID_GP",
            "false, false, true, false,  IDAM_USER_ID",
            "false, false, true, false,  IDAM_USER_ID_GP",
            "false, false, true, false, null",
            "false, false, false, true,  IDAM_USER_ID_GP",
        })
        void should_succeed_and_return_204_and_update_cft_task_state_gp_flag_on(boolean ownPermission,
                                                                                boolean executePermission, boolean completePermission, boolean completeOwnPermission, String assignee) throws Exception {
            if (assignee.equals("null")) {
                assignee = null;
            }

            mockServices.mockServiceAPIsGp();
            when(mockedUserInfo.getUid())
                .thenReturn(IDAM_USER_ID_GP);

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("WA")
                        .caseType("WaCaseType")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, ownPermission, executePermission, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
                taskId, OffsetDateTime.now(),
                completePermission, completeOwnPermission, false, false, false, false,
                false, false, false, false
            );
            insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource, assignee);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

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
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isNoContent());

            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

            assertTrue(taskResource.isPresent());
            assertEquals(COMPLETED, taskResource.get().getState());
            assertEquals(assignee, taskResource.get().getAssignee());
            assertNotNull(taskResource.get().getLastUpdatedTimestamp());
            assertEquals(IDAM_USER_ID_GP, taskResource.get().getLastUpdatedUser());
            assertEquals(TaskAction.COMPLETED.getValue(), taskResource.get().getLastUpdatedAction());
        }

        @ParameterizedTest
        @CsvSource(value = {
            "false, false, false, false, IDAM_USER_ID_GP",
            "true, false, false, false, IDAM_USER_ID",
            "false, true, false, false, IDAM_USER_ID",
            "false, false, false, true,  IDAM_USER_ID",
        })
        void should_succeed_and_return_403_when_no_perms_or_not_assignee_gp_flag_on(boolean ownPermission,
                                                                                    boolean executePermission, boolean completePermission, boolean completeOwnPermission, String assignee) throws Exception {

            mockServices.mockServiceAPIsGp();
            when(mockedUserInfo.getUid())
                .thenReturn(IDAM_USER_ID_GP);

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
                .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
                .roleAssignmentAttribute(
                    RoleAssignmentAttribute.builder()
                        .jurisdiction("WA")
                        .caseType("WaCaseType")
                        .caseId("completeCaseId1")
                        .build()
                )
                .build();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

            TaskRoleResource taskRoleResource = new TaskRoleResource(
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
                false, ownPermission, executePermission, false, false, false,
                new String[]{}, 1, false,
                TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
                taskId, OffsetDateTime.now(),
                completePermission, completeOwnPermission, false, false, false, false,
                false, false, false, false
            );
            insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource, assignee);

            when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

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
                        .contentType(APPLICATION_JSON_VALUE)
                )
                .andExpect(status().isForbidden());
        }

    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId, TaskRoleResource taskRoleResource) {
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource, IDAM_USER_ID);
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId, TaskRoleResource taskRoleResource,
                                     String assignee) {
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
        taskResource.setCaseId("completeCaseId1");
        taskResource.setAssignee(assignee);
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void insertUnassignTaskInDb(String jurisdiction, String caseType, String taskId, TaskRoleResource taskRoleResource) {
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
        taskResource.setCaseId("completeCaseId1");
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "/task/complete",
            Collections.emptyMap(),
            new byte[0],
            StandardCharsets.UTF_8,
            null
        );

        Response response = Response.builder()
            .status(status)
            .request(request)
            .headers(Collections.emptyMap())
            .build();

        return FeignException.errorStatus("camunda", response);
    }
}
