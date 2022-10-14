package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_OTHER_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:LineLength")
class PostTaskCompleteByIdControllerTest extends SpringBootIntegrationBaseTest {

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
        void should_succeed_and_return_204_and_update_cft_task_state() throws Exception {

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
        void should_return_a_403_when_the_user_did_not_have_sufficient_jurisdiction_did_not_match() throws Exception {

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
        void should_return_a_403_when_user_jurisdiction_did_not_match_and_assign_and_complete_tru() throws Exception {

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

            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
                .thenReturn(new AccessControlResponse(mockedUserInfo,
                                                      roleAssignmentsWithJurisdiction));

            when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
            when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

            CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
            mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(APPLICATION_JSON_VALUE)
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
        taskResource.setCaseId("completeCaseId1");
        taskResource.setAssignee(IDAM_USER_ID);
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }
}

