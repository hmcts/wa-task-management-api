package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostUpdateTaskWithNotesControllerFailureTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/notes";
    private static String ENDPOINT_BEING_TESTED;

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private TaskManagementService taskManagementService;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private ClientAccessControlService clientAccessControlService;

    @Mock
    TaskResource taskResource = mock((TaskResource.class));

    private ServiceMocks mockServices;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
        when(clientAccessControlService.hasExclusiveAccess(any()))
            .thenReturn(true);

        when(taskManagementService.getTaskById(any()))
            .thenReturn(Optional.of(taskResource));
    }

    @Test
    void should_return_404_when_task_not_found() throws Exception {
        mockServices.mockServiceAPIs();
        when(clientAccessControlService.hasExclusiveAccess(any()))
            .thenReturn(true);

        when(taskManagementService.getTaskById(eq(taskId))).thenReturn(Optional.empty());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(addNotes()))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
                jsonPath("$.title").value("Task Not Found Error"),
                jsonPath("$.status").value(404),
                jsonPath("$.detail").value(
                    "Task Not Found Error: The task could not be found.")
            ));
    }

    @Test
    void should_return_403_when_service_token_is_wrong() throws Exception {
        mockServices.mockServiceAPIs();

        when(clientAccessControlService.hasExclusiveAccess(any()))
            .thenReturn(false);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(addNotes()))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/forbidden"),
                jsonPath("$.title").value("Forbidden"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Forbidden: The action could not be completed "
                    + "because the client/user had insufficient rights to a resource.")
            ));
    }

    @Test
    void should_return_400_when_no_note_request_is_not_given() throws Exception {
        mockServices.mockServiceAPIs();

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new InvalidRequestException("Invalid request message")
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                jsonPath("$.title").value("Bad Request"),
                jsonPath("$.status").value(400)));
    }

    @Test
    void should_return_400_when_no_note_resource() throws Exception {
        mockServices.mockServiceAPIs();

        NotesRequest notesRequest = new NotesRequest(
            emptyList()
        );

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new CustomConstraintViolationException(
                List.of(new Violation(
                        "note_resource",
                        "must not be empty"
                    )
                )
            )
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(notesRequest))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
                jsonPath("$.title").value("Constraint Violation"),
                jsonPath("$.status").value(400),
                jsonPath("$.violations").isNotEmpty(),
                jsonPath("$.violations.[0].field").value("note_resource"),
                jsonPath("$.violations.[0].message")
                    .value("must not be empty")));
    }

    @Test
    void should_return_400_when_no_note_resource_is_null() throws Exception {
        mockServices.mockServiceAPIs();

        NotesRequest notesRequest = new NotesRequest(
            null
        );

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new CustomConstraintViolationException(
                List.of(new Violation(
                        "note_resource",
                        "must not be empty"
                    )
                )
            )
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(notesRequest))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
                jsonPath("$.title").value("Constraint Violation"),
                jsonPath("$.status").value(400),
                jsonPath("$.violations").isNotEmpty(),
                jsonPath("$.violations.[0].field").value("note_resource"),
                jsonPath("$.violations.[0].message")
                    .value("must not be empty")));
    }

    @Test
    void should_return_400_when_note_request_is_null() throws Exception {
        mockServices.mockServiceAPIs();

        NotesRequest notesRequest = null;

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new InvalidRequestException("Invalid request message")
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(notesRequest))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                jsonPath("$.title").value("Bad Request"),
                jsonPath("$.status").value(400)));

    }

    @Test
    void should_return_400_when_no_note_code_type() throws Exception {
        mockServices.mockServiceAPIs();

        NotesRequest notesRequest = new NotesRequest(
            singletonList(
                new NoteResource(null, "someNoteType", null, null)
            )
        );

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new CustomConstraintViolationException(
                List.of(new Violation(
                        "code",
                        "must not be empty"
                    )
                )
            )
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(notesRequest))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
                jsonPath("$.title").value("Constraint Violation"),
                jsonPath("$.status").value(400),
                jsonPath("$.violations").isNotEmpty(),
                jsonPath("$.violations.[0].field").value("code"),
                jsonPath("$.violations.[0].message")
                    .value("must not be empty")));
    }

    @Test
    void should_return_400_when_no_note_note_type() throws Exception {
        mockServices.mockServiceAPIs();

        NotesRequest notesRequest = new NotesRequest(
            singletonList(
                new NoteResource("someCodeType", null, null, null)
            )
        );

        given(taskManagementService.updateNotes(any(), any())).willThrow(
            new CustomConstraintViolationException(
                List.of(new Violation(
                        "note_type",
                        "must not be empty"
                    )
                )
            )
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(notesRequest))
        ).andExpect(
            ResultMatcher.matchAll(
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"),
                jsonPath("$.title").value("Constraint Violation"),
                jsonPath("$.status").value(400),
                jsonPath("$.violations").isNotEmpty(),
                jsonPath("$.violations.[0].field").value("note_type"),
                jsonPath("$.violations.[0].message")
                    .value("must not be empty")));
    }

    private NotesRequest addNotes() {
        return new NotesRequest(
            singletonList(
                new NoteResource("code", "someNoteType", "userId", "content")
            )
        );
    }
}

