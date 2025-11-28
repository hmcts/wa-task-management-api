package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TerminationProcessHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
class DeleteTerminateByIdControllerTest {
    private static final String ENDPOINT_PATH = "/task/%s";
    public static final String SYSTEM_USER_1 = "system_user1";
    private static String ENDPOINT_BEING_TESTED;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;
    @Autowired
    private TaskManagementService taskManagementService;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockitoBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private TerminationProcessHelper terminationProcessHelper;
    @Autowired
    private IdamTokenGenerator systemUserIdamToken;
    @Autowired
    IntegrationTestUtils integrationTestUtils;
    @Autowired
    protected MockMvc mockMvc;

    TaskTestUtils taskTestUtils;

    private String bearerAccessToken1;

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService,"primary");
    }

    @BeforeEach
    void setUp() {
        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
    }

    @Nested
    @DisplayName("Terminate reason is cancelled")
    class Cancelled {

        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            String taskId = UUID.randomUUID().toString();

            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("cancelled"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(req))
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
            String taskId = taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId1",
                                                                       null,null);
            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
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
                    .content(integrationTestUtils.asJsonString(req))
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

            String taskId = UUID.randomUUID().toString();

            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("completed"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(req))
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
            String taskId = taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId2",
                                                                       null,null);
            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
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
                    .content(integrationTestUtils.asJsonString(req))
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

            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, "dummyTaskId");

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("deleted"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(req))
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
            String taskId = taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "deleteTerminateByIdCaseId3",
                                                                       null,null);
            ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
            when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
            when(camundaServiceApi.searchHistory(eq(SERVICE_AUTHORIZATION_TOKEN), any())).thenReturn(emptyList());
            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);
            when(terminationProcessHelper.fetchTerminationProcessFromCamunda(anyString()))
                .thenReturn(Optional.empty());
            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo("deleted"));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(integrationTestUtils.asJsonString(req))
            ).andExpectAll(
                status().isNoContent()
            );


            Optional<TaskResource> taskInDb = cftTaskDatabaseService.findByIdOnly(taskId);
            assertTrue(taskInDb.isPresent());
            assertEquals(CFTTaskState.TERMINATED, taskInDb.get().getState());
            assertEquals("deleted", taskInDb.get().getTerminationReason());
            assertEquals(SYSTEM_USER_1, taskInDb.get().getLastUpdatedUser());
            assertEquals(TaskAction.TERMINATE_EXCEPTION.getValue(), taskInDb.get().getLastUpdatedAction());
            assertNotNull(taskInDb.get().getLastUpdatedTimestamp());
        }
    }
}

