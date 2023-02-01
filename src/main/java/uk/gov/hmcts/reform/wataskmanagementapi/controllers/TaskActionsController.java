package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskRolePermissionsResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CyclomaticComplexity", "PMD.AvoidDuplicateLiterals"})
public class TaskActionsController extends BaseController {
    private static final Logger LOG = getLogger(TaskActionsController.class);

    private final TaskManagementService taskManagementService;
    private final AccessControlService accessControlService;
    private final ClientAccessControlService clientAccessControlService;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final SystemDateProvider systemDateProvider;

    @Autowired
    public TaskActionsController(TaskManagementService taskManagementService,
                                 AccessControlService accessControlService,
                                 SystemDateProvider systemDateProvider,
                                 ClientAccessControlService clientAccessControlService,
                                 LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
        this.systemDateProvider = systemDateProvider;
        this.clientAccessControlService = clientAccessControlService;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
    }

    @Operation(description = "Retrieve a Task Resource identified by its unique id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK, content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = GetTaskResponse.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @GetMapping(path = "/{task-id}")
    public ResponseEntity<GetTaskResponse<Task>> getTask(@RequestHeader(AUTHORIZATION) String authToken,
                                                         @PathVariable(TASK_ID) String id) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        Task task = taskManagementService.getTask(
            id,
            accessControlResponse
        );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskResponse<>(task));
    }

    @Operation(description = "Claim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = NO_CONTENT, content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/claim")
    public ResponseEntity<Void> claimTask(@RequestHeader(AUTHORIZATION) String authToken,
                                          @PathVariable(TASK_ID) String taskId) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        taskManagementService.claimTask(taskId, accessControlResponse);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();

    }

    @Operation(description = "Unclaim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task unclaimed", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/unclaim")
    public ResponseEntity<Void> unclaimTask(@RequestHeader(AUTHORIZATION) String authToken,
                                            @PathVariable(TASK_ID) String taskId) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        taskManagementService.unclaimTask(taskId, accessControlResponse);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Assign the identified Task to a specified user.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task assigned", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<Void> assignTask(@RequestHeader(AUTHORIZATION) String assignerAuthToken,
                                           @PathVariable(TASK_ID) String taskId,
                                           @RequestBody AssignTaskRequest assignTaskRequest) {

        AccessControlResponse assignerAccessControlResponse = accessControlService.getRoles(assignerAuthToken);
        Optional<AccessControlResponse> assigneeAccessControlResponse = getAssigneeAccessControlResponse(
            assignerAuthToken,
            assignTaskRequest,
            assignerAccessControlResponse
        );

        taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        );
        return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
    }

    @Operation(description = "Completes a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task has been completed", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/complete")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> completeTask(@RequestHeader(AUTHORIZATION) String authToken,
                                             @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                             @PathVariable(TASK_ID) String taskId,
                                             @RequestBody(required = false) CompleteTaskRequest completeTaskRequest) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        if (completeTaskRequest == null || completeTaskRequest.getCompletionOptions() == null) {
            taskManagementService.completeTask(taskId, accessControlResponse);
        } else {
            boolean isPrivilegedRequest =
                clientAccessControlService.hasPrivilegedAccess(serviceAuthToken, accessControlResponse);

            if (isPrivilegedRequest) {
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    completeTaskRequest.getCompletionOptions()
                );
            } else {
                throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
            }
        }
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Cancel a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task has been cancelled", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/cancel")
    public ResponseEntity<Void> cancelTask(@RequestHeader(AUTHORIZATION) String authToken,
                                           @PathVariable(TASK_ID) String taskId) {
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        taskManagementService.cancelTask(taskId, accessControlResponse);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Update Task with notes")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Updated Task with notes", content = {
            @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST, content = {
            @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "404", description = NOT_FOUND, content = {
            @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Object.class))}),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/notes")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> updatesTaskWithNotes(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
        @PathVariable(TASK_ID) String taskId,
        @RequestBody NotesRequest notesRequest
    ) {

        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthToken);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        Optional<TaskResource> optionalTaskResource = taskManagementService.getTaskById(taskId);
        if (optionalTaskResource.isEmpty()) {
            throw new TaskNotFoundException(ErrorMessages.TASK_NOT_FOUND_ERROR);
        }

        taskManagementService.updateNotes(taskId, notesRequest);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Retrieve the role permissions information for the task identified by the given task-id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK, content = {@Content(mediaType = "application/json",
                schema = @Schema(implementation = GetTaskRolePermissionsResponse.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @GetMapping(path = "/{task-id}/roles")
    public ResponseEntity<GetTaskRolePermissionsResponse> getTaskRolePermissions(
        @RequestHeader(AUTHORIZATION) String authToken, @PathVariable(TASK_ID) String id) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
            id,
            accessControlResponse
        );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskRolePermissionsResponse(taskRolePermissions));
    }

    @ExceptionHandler(NoRoleAssignmentsFoundException.class)
    public ResponseEntity<ErrorMessage> handleNoRoleAssignmentsException(Exception ex) {
        LOG.warn("No role assignments found");
        return status(HttpStatus.UNAUTHORIZED)
            .cacheControl(CacheControl.noCache())
            .body(new ErrorMessage(
                ex,
                HttpStatus.UNAUTHORIZED,
                systemDateProvider.nowWithTime()
            ));
    }

    private Optional<AccessControlResponse> getAssigneeAccessControlResponse(
        String assignerAuthToken,
        AssignTaskRequest assignTaskRequest,
        AccessControlResponse assignerAccessControlResponse) {

        UserInfo userInfo = assignerAccessControlResponse.getUserInfo();
        return assignTaskRequest.getUserId() == null
            && isGranularPermissionFeatureEnabled(userInfo.getUid(), userInfo.getEmail())
            ? Optional.empty()
            : Optional.ofNullable(accessControlService.getRolesGivenUserId(
            assignTaskRequest.getUserId(),
            assignerAuthToken
        ));
    }

    private boolean isGranularPermissionFeatureEnabled(String userId, String email) {
        return launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.GRANULAR_PERMISSION_FEATURE,
                userId,
                email
            );
    }
}
