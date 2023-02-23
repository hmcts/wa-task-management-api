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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskRolePermissionsResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
    private RoleAssignment mockedAssigneeRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private UserInfo mockedAssigneeUserInfo;
    @Mock
    private SystemDateProvider systemDateProvider;
    @Mock
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private TaskActionsController taskActionsController;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        taskActionsController = new TaskActionsController(
            taskManagementService,
            accessControlService,
            systemDateProvider,
            clientAccessControlService,
            launchDarklyFeatureFlagProvider
        );

    }

    @Test
    void should_succeed_when_fetching_a_task_and_return_a_204_no_content() {

        Task mockedTask = mock(Task.class);
        AccessControlResponse mockAccessControlResponse = new AccessControlResponse(
            mockedUserInfo,
            singletonList(mockedRoleAssignment)
        );

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockAccessControlResponse);

        when(taskManagementService.getTask(
            taskId,
            mockAccessControlResponse
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
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        ResponseEntity<Void> response = taskActionsController.claimTask(IDAM_AUTH_TOKEN, taskId);

        verify(taskManagementService, times(1))
            .claimTask(taskId, mockAccessControlResponse);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_unclaim_a_task_204_no_content() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        ResponseEntity<Void> response = taskActionsController.unclaimTask(IDAM_AUTH_TOKEN, taskId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(taskManagementService, times(1))
            .unclaimTask(taskId, mockAccessControlResponse);
    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_assigning_task() {
        AssignTaskRequest assignTaskRequest = new AssignTaskRequest("userId");

        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockAccessControlResponse);

        AccessControlResponse mockedAssigneeAccessControlResponse =
            new AccessControlResponse(mockedAssigneeUserInfo, singletonList(mockedAssigneeRoleAssignment));
        when(accessControlService.getRolesGivenUserId(assignTaskRequest.getUserId(), IDAM_AUTH_TOKEN))
            .thenReturn(mockedAssigneeAccessControlResponse);

        ResponseEntity<Void> response = taskActionsController.assignTask(
            IDAM_AUTH_TOKEN,
            taskId,
            assignTaskRequest
        );

        verify(taskManagementService, times(1))
            .assignTask(taskId, mockAccessControlResponse, Optional.of(mockedAssigneeAccessControlResponse));

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_assigning_task_for_null_assignee_for_granular() {
        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(null);

        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockAccessControlResponse);

        String assignerId = "assignerId";
        when(mockedUserInfo.getUid()).thenReturn(assignerId);
        String assignerEmail = "assignerEmail";
        when(mockedUserInfo.getEmail()).thenReturn(assignerEmail);
        when(launchDarklyFeatureFlagProvider
                 .getBooleanValue(FeatureFlag.GRANULAR_PERMISSION_FEATURE, assignerId, assignerEmail))
            .thenReturn(true);

        ResponseEntity<Void> response = taskActionsController.assignTask(
            IDAM_AUTH_TOKEN,
            taskId,
            assignTaskRequest
        );

        verify(taskManagementService, times(1))
            .assignTask(taskId, mockAccessControlResponse, Optional.empty());

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_fail_and_return_a_204_no_content_when_assigning_task_for_null_assignee_for_non_granular() {
        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(null);

        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockAccessControlResponse);

        when(accessControlService.getRolesGivenUserId(null, IDAM_AUTH_TOKEN))
            .thenThrow(new NullPointerException("IdamUserId cannot be null"));

        String assignerId = "assignerId";
        when(mockedUserInfo.getUid()).thenReturn(assignerId);
        String assignerEmail = "assignerEmail";
        when(mockedUserInfo.getEmail()).thenReturn(assignerEmail);
        when(launchDarklyFeatureFlagProvider
                 .getBooleanValue(FeatureFlag.GRANULAR_PERMISSION_FEATURE, assignerId, assignerEmail))
            .thenReturn(false);

        Throwable exception = assertThrows(
            NullPointerException.class,
            () -> taskActionsController.assignTask(
                IDAM_AUTH_TOKEN,
                taskId,
                assignTaskRequest
            )
        );

        assertEquals("IdamUserId cannot be null", exception.getMessage());
        verify(taskManagementService, times(0))
            .assignTask(taskId, mockAccessControlResponse, Optional.empty());

    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_unassigning_task() {
        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(null);

        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(mockAccessControlResponse);


        ResponseEntity<Void> response = taskActionsController.assignTask(
            IDAM_AUTH_TOKEN,
            taskId,
            assignTaskRequest
        );

        verify(accessControlService, never()).getRolesGivenUserId(anyString(), eq(IDAM_AUTH_TOKEN));

        verify(taskManagementService, times(1))
            .assignTask(taskId, mockAccessControlResponse, Optional.empty());

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
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        ResponseEntity response = taskActionsController.cancelTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(taskManagementService, times(1))
            .cancelTask(taskId, mockAccessControlResponse);
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

    @Test
    void should_update_notes() {
        final NotesRequest notesRequest = addNotes();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
        TaskResource taskResource = spy(TaskResource.class);
        when(taskManagementService.getTaskById(taskId)).thenReturn(Optional.of(taskResource));
        when(taskManagementService.updateNotes(taskId, notesRequest)).thenReturn(any());
        ResponseEntity<Void> response = taskActionsController
            .updatesTaskWithNotes(SERVICE_AUTHORIZATION_TOKEN, taskId, notesRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(taskManagementService, times(1))
            .updateNotes(taskId, notesRequest);
    }

    @Test
    void should_return_403_when_task_updated_with_notes_and_with_insufficient_permission() {
        NotesRequest notesRequest = addNotes();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        assertThatThrownBy(() -> taskActionsController
            .updatesTaskWithNotes(SERVICE_AUTHORIZATION_TOKEN, taskId, notesRequest))
            .isInstanceOf(GenericForbiddenException.class)
            .hasNoCause()
            .hasMessage("Forbidden: "
                        + "The action could not be completed because the "
                        + "client/user had insufficient rights to a resource.");

        verify(taskManagementService, times(0))
            .updateNotes(taskId, notesRequest);
    }

    @Test
    void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";
        NotesRequest notesRequest = addNotes();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        assertThatThrownBy(() -> taskActionsController
            .updatesTaskWithNotes(SERVICE_AUTHORIZATION_TOKEN, nonExistentTaskId, notesRequest))
            .isInstanceOf(TaskNotFoundException.class)
            .hasNoCause()
            .hasMessage("Task Not Found Error: The task could not be found.");

        verify(taskManagementService, times(0))
            .updateNotes(taskId, notesRequest);
    }

    @Test
    void should_return_task_roles_with_permissions() {
        AccessControlResponse mockAccessControlResponse =
            new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment));
        when(accessControlService.getRoles(IDAM_AUTH_TOKEN)).thenReturn(mockAccessControlResponse);

        List<PermissionTypes> permissionTypes = List.of(PermissionTypes.READ);
        List<String> authorisations = List.of("Divorce");
        List<TaskRolePermissions> taskRolePermissions = List.of(
            new TaskRolePermissions("roleCategory", "roleName", permissionTypes, authorisations));
        when(taskManagementService.getTaskRolePermissions(taskId, mockAccessControlResponse))
            .thenReturn(taskRolePermissions);
        final ResponseEntity<GetTaskRolePermissionsResponse> response = taskActionsController
            .getTaskRolePermissions(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getPermissionsList(), taskRolePermissions);
    }

    private NotesRequest addNotes() {
        NoteResource noteResource = new NoteResource(
            "code", "notetype", "userId", "content");
        return new NotesRequest(List.of(noteResource));
    }
}
