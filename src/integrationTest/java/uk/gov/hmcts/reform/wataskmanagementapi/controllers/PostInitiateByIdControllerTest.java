package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostInitiateByIdControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task/%s";
    private static String ENDPOINT_BEING_TESTED;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

    }

    @AfterAll
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void should_return_403_with_application_problem_response_when_client_is_not_allowed() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName")
        ));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
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
    void should_return_201_with_task_unassigned() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName")
        ));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req))
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isCreated(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.task_id").value(taskId),
                    jsonPath("$.task_type").value("aTaskType"),
                    jsonPath("$.task_name").value("aTaskName"),
                    jsonPath("$.state").value("UNASSIGNED"),
                    jsonPath("$.auto_assigned").value(false),
                    jsonPath("$.has_warnings").value("false")
                ));
    }


    @Test
    void should_return_201_with_task_assigned() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_NAME, "aTaskName")
        ));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req))
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isCreated(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.task_id").value(taskId),
                    jsonPath("$.task_type").value("aTaskType"),
                    jsonPath("$.task_name").value("aTaskName"),
                    jsonPath("$.state").value("ASSIGNED"),
                    jsonPath("$.assignee").value("someAssignee"),
                    jsonPath("$.auto_assigned").value(false),
                    jsonPath("$.has_warnings").value("false")
                ));
    }

    @Test
    void should_return_201_with_task_state_from_attributes() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_STATE, "UNCONFIGURED"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_NAME, "aTaskName")
        ));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(req))
        ).andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isCreated(),
                    content().contentType(APPLICATION_JSON_VALUE),
                    jsonPath("$.task_id").value(taskId),
                    jsonPath("$.task_type").value("aTaskType"),
                    jsonPath("$.task_name").value("aTaskName"),
                    jsonPath("$.state").value("UNCONFIGURED"),
                    jsonPath("$.assignee").value("someAssignee"),
                    jsonPath("$.auto_assigned").value(false),
                    jsonPath("$.has_warnings").value("false")
                ));
    }
}

