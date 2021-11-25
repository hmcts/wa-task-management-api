package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class DeleteTerminateByIdControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task/%s";
    private static String ENDPOINT_BEING_TESTED;

    @MockBean
    private ClientAccessControlService clientAccessControlService;

    @MockBean
    private TaskManagementService taskManagementService;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
    }


    @Nested
    @DisplayName("Terminate reason is cancelled")
    class Cancelled {
        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.CANCELLED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                    jsonPath("$.title").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Forbidden: The action could not be completed because the client/user "
                        + "had insufficient rights to a resource.")
                ));
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.CANCELLED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isNoContent()
                ));
        }

    }

    @Nested
    @DisplayName("Terminate reason is completed")
    class Completed {
        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.COMPLETED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                    jsonPath("$.title").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Forbidden: The action could not be completed because the client/user "
                        + "had insufficient rights to a resource.")
                ));
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.COMPLETED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isNoContent()
                ));
        }
    }

    @Nested
    @DisplayName("Terminate reason is deleted")
    class Deleted {
        @Test
        void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.DELETED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isForbidden(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                    jsonPath("$.title").value("Forbidden"),
                    jsonPath("$.status").value(403),
                    jsonPath("$.detail").value(
                        "Forbidden: The action could not be completed because the client/user "
                        + "had insufficient rights to a resource.")
                ));
        }

        @Test
        void should_return_204_and_delete_task() throws Exception {

            when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

            TerminateTaskRequest req = new TerminateTaskRequest(new TerminateInfo(TerminateReason.DELETED));

            mockMvc.perform(
                delete(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(asJsonString(req))
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isNoContent()
                ));
        }
    }
}

