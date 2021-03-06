package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@ExtendWith(MockitoExtension.class)
class TaskActionsControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "SERVICE_AUTHORIZATION_TOKEN";
    @Mock
    private TaskManagementService taskManagementService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RoleAssignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private SystemDateProvider systemDateProvider;
    @Mock
    private ClientAccessControlService clientAccessControlService;

    private TaskActionsController taskActionsController;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        taskActionsController = new TaskActionsController(
            taskManagementService,
            accessControlService,
            systemDateProvider,
            clientAccessControlService
        );

    }

    @Test
    void should_succeed_when_fetching_a_task_and_return_a_204_no_content() {

        Task mockedTask = mock(Task.class);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        when(taskManagementService.getTask(
            taskId,
            singletonList(mockedRoleAssignment)
        ))
            .thenReturn(mockedTask);

        ResponseEntity<GetTaskResponse<Task>> response = taskActionsController.getTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetTaskResponse.class));
        assertNotNull(response.getBody());
        assertEquals(mockedTask, response.getBody().getTask());
    }

    @Test
    void should_succeed_when_claiming_a_task_and_return_a_204_no_content() {

        ResponseEntity<Void> response = taskActionsController.claimTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_unclaim_a_task_204_no_content() {

        String authToken = "someAuthToken";

        ResponseEntity<Void> response = taskActionsController.unclaimTask(authToken, taskId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_assigning_task() {

        String authToken = "someAuthToken";

        ResponseEntity<Void> response = taskActionsController.assignTask(
            authToken,
            taskId,
            new AssignTaskRequest("userId")
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_complete_a_task_with_no_extra_body_parameters_and_no_privileged_access() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        ResponseEntity response = taskActionsController.completeTask(
            IDAM_AUTH_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            taskId,
            null
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManagementService, times(1))
            .completeTask(taskId, mockAccessControlResponse);

    }

    @Test
    void should_complete_a_task_with_extra_body_parameters_and_privileged_access() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN, mockAccessControlResponse))
            .thenReturn(true);

        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));

        ResponseEntity response = taskActionsController.completeTask(
            IDAM_AUTH_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            taskId,
            request
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManagementService, times(1)).completeTaskWithPrivilegeAndCompletionOptions(
            taskId,
            mockAccessControlResponse,
            request.getCompletionOptions()
        );

    }

    @Test
    void should_complete_a_task_with_extra_body_parameters_and_null_completion_options() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        CompleteTaskRequest request = new CompleteTaskRequest(null);

        ResponseEntity response = taskActionsController.completeTask(
            IDAM_AUTH_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            taskId,
            request
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManagementService, times(1)).completeTask(
            taskId,
            mockAccessControlResponse
        );

    }

    @Test
    void should_return_403_throw_insufficient_permission_when_extra_body_parameters_and_no_privileged_access() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN, mockAccessControlResponse))
            .thenReturn(false);


        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));

        assertThatThrownBy(() -> taskActionsController.completeTask(
            IDAM_AUTH_TOKEN,
            SERVICE_AUTHORIZATION_TOKEN,
            taskId,
            request
        ))
            .isInstanceOf(GenericForbiddenException.class)
            .hasNoCause()
            .hasMessage("Forbidden: "
                        + "The action could not be completed because the "
                        + "client/user had insufficient rights to a resource.");

    }

    @Test
    void should_cancel_a_task() {

        ResponseEntity response = taskActionsController.cancelTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_return_403_when_no_role_assignments_are_found() {

        final String exceptionMessage = "Some exception message";
        final NoRoleAssignmentsFoundException exception =
            new NoRoleAssignmentsFoundException(exceptionMessage);

        String mockedTimestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = taskActionsController.handleNoRoleAssignmentsException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());

    }
}
