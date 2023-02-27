package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class DeleteTerminateByIdControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task/%s";
    public static final String SYSTEM_USER_1 = "system_user1";
    private static String ENDPOINT_BEING_TESTED;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Autowired
    private TaskManagementService taskManagementService;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @MockBean
    private IdamWebApi idamWebApi;
    @Autowired
    private IdamTokenGenerator systemUserIdamToken;
    private String taskId;
    private String bearerAccessToken1;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
    }

    @Nested
    @DisplayName("Terminate reason is cancelled")
    class Cancelled {

        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("cancelled"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(
                status().isForbidden(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                jsonPath("$.title").value("Forbidden"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Forbidden: The action could not be completed because the client/user "
                    + "had insufficient rights to a resource.")
            );
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {
            createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId1");
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            when(camundaServiceApi.searchHistory(eq(SERVICE_AUTHORIZATION_TOKEN), any())).thenReturn(emptyList());
            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("cancelled"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(
                status().isNoContent()
            );


            Optional<TaskResource> taskInDb = cftTaskDatabaseService.findByIdOnly(taskId);
            assertTrue(taskInDb.isPresent());
            assertEquals(CFTTaskState.TERMINATED, taskInDb.get().getState());
            assertEquals("cancelled", taskInDb.get().getTerminationReason());
            assertEquals(SYSTEM_USER_1, taskInDb.get().getLastUpdatedUser());
            assertEquals(TaskAction.AUTO_CANCEL.getValue(), taskInDb.get().getLastUpdatedAction());
            assertNotNull(taskInDb.get().getLastUpdatedTimestamp());
        }


    }

    @Nested
    @DisplayName("Terminate reason is completed")
    class Completed {
        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("completed"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(

                status().isForbidden(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                jsonPath("$.title").value("Forbidden"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Forbidden: The action could not be completed because the client/user "
                    + "had insufficient rights to a resource.")
            );
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {
            createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId2");
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            when(camundaServiceApi.searchHistory(eq(SERVICE_AUTHORIZATION_TOKEN), any())).thenReturn(emptyList());

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("completed"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(
                status().isNoContent()
            );


            Optional<TaskResource> taskInDb = cftTaskDatabaseService.findByIdOnly(taskId);
            assertTrue(taskInDb.isPresent());
            assertEquals(CFTTaskState.TERMINATED, taskInDb.get().getState());
            assertEquals("completed", taskInDb.get().getTerminationReason());
            assertEquals(SYSTEM_USER_1, taskInDb.get().getLastUpdatedUser());
            assertEquals(TaskAction.TERMINATE.getValue(), taskInDb.get().getLastUpdatedAction());
            assertNotNull(taskInDb.get().getLastUpdatedTimestamp());
        }
    }

    @Nested
    @DisplayName("Terminate reason is deleted")
    class Deleted {
        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("deleted"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(

                status().isForbidden(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                jsonPath("$.title").value("Forbidden"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Forbidden: The action could not be completed because the client/user "
                    + "had insufficient rights to a resource.")
            );
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {
            createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId3");
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            when(camundaServiceApi.searchHistory(eq(SERVICE_AUTHORIZATION_TOKEN), any())).thenReturn(emptyList());
            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("deleted"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpectAll(
                status().isNoContent()
            );


            Optional<TaskResource> taskInDb = cftTaskDatabaseService.findByIdOnly(taskId);
            assertTrue(taskInDb.isPresent());
            assertEquals(CFTTaskState.TERMINATED, taskInDb.get().getState());
            assertEquals("deleted", taskInDb.get().getTerminationReason());
            assertEquals(SYSTEM_USER_1, taskInDb.get().getLastUpdatedUser());
            assertNotNull(taskInDb.get().getLastUpdatedTimestamp());
        }
    }

    private void insertDummyTaskInDb(String jurisdiction,
                                     String caseType,
                                     String caseId,
                                     String taskId, CFTTaskState cftTaskState,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void createTaskAndRoleAssignments(CFTTaskState cftTaskState, String caseId) {
        //assigner permission : manage, own, cancel
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, true, true, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );
        String jurisdiction = "IA";
        String caseType = "Asylum";
        insertDummyTaskInDb(jurisdiction, caseType, caseId, taskId, cftTaskState, assignerTaskRoleResource);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
    }
}

