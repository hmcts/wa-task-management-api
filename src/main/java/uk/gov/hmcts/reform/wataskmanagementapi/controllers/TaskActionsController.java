package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskRolePermissionsResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CompletionProcessValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskDeletionService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.InputParamsVerifier.verifyCaseId;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.ResponseEntityBuilder.buildErrorResponseEntityAndLogError;

@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@Slf4j
@RestController
@RefreshScope
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CyclomaticComplexity", "PMD.AvoidDuplicateLiterals","PMD.LawOfDemeter"})
public class TaskActionsController extends BaseController {
    public static final String REQ_PARAM_COMPLETION_PROCESS = "completion_process";
    public static final String REQ_PARAM_CANCELLATION_PROCESS = "cancellation_process";

    private static final Logger LOG = getLogger(TaskActionsController.class);

    private final TaskManagementService taskManagementService;
    private final AccessControlService accessControlService;
    private final ClientAccessControlService clientAccessControlService;
    private final SystemDateProvider systemDateProvider;

    private final TaskDeletionService taskDeletionService;

    private final CompletionProcessValidator completionProcessValidator;

    private final CancellationProcessValidator cancellationProcessValidator;

    @Autowired
    public TaskActionsController(TaskManagementService taskManagementService,
                                 AccessControlService accessControlService,
                                 SystemDateProvider systemDateProvider,
                                 ClientAccessControlService clientAccessControlService,
                                 TaskDeletionService taskDeletionService,
                                 CompletionProcessValidator completionProcessValidator,
                                 CancellationProcessValidator cancellationProcessValidator) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
        this.systemDateProvider = systemDateProvider;
        this.clientAccessControlService = clientAccessControlService;
        this.taskDeletionService = taskDeletionService;
        this.completionProcessValidator = completionProcessValidator;
        this.cancellationProcessValidator = cancellationProcessValidator;
    }

    @Operation(description = "Retrieve a Task Resource identified by its unique id.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "200", description = OK, content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = GetTaskResponse.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @GetMapping(path = "/{task-id}", consumes = {ALL_VALUE})
    public ResponseEntity<GetTaskResponse<Task>> getTask(@Parameter(hidden = true)
                                                             @RequestHeader(AUTHORIZATION) String authToken,
                                                         @PathVariable(TASK_ID) String id) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Get task request for task-id {}, user {}", id,
            accessControlResponse.getUserInfo().getUid());

        Task task = taskManagementService.getTask(
            id,
            accessControlResponse
        );

        if (!completionProcessValidator.isCompletionProcessFeatureEnabled(accessControlResponse)) {
            task.setTerminationProcess(null);
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskResponse<>(task));
    }

    @Operation(description = "Claim the identified Task for the currently logged in user.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "204", description = NO_CONTENT, content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/claim")
    public ResponseEntity<Void> claimTask(@Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
                                          @PathVariable(TASK_ID) String taskId) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Claim task request for task-id {}, user {}", taskId,
            accessControlResponse.getUserInfo().getUid());

        taskManagementService.claimTask(taskId, accessControlResponse);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();

    }

    @Operation(description = "Unclaim the identified Task for the currently logged in user.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "204", description = "Task unclaimed", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/unclaim")
    public ResponseEntity<Void> unclaimTask(@Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
                                            @PathVariable(TASK_ID) String taskId) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Unclaim task request for task-id {}, user {}", taskId,
            accessControlResponse.getUserInfo().getUid());

        taskManagementService.unclaimTask(taskId, accessControlResponse);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Assign the identified Task to a specified user.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "204", description = "Task assigned", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<Void> assignTask(@Parameter(hidden = true)
                                               @RequestHeader(AUTHORIZATION) String assignerAuthToken,
                                           @PathVariable(TASK_ID) String taskId,
                                           @RequestBody AssignTaskRequest assignTaskRequest) {

        AccessControlResponse assignerAccessControlResponse = accessControlService.getRoles(assignerAuthToken);
        Optional<AccessControlResponse> assigneeAccessControlResponse = getAssigneeAccessControlResponse(
            assignerAuthToken,
            assignTaskRequest
        );

        LOG.info("Task Action: Assign task request for task-id {}, user {}, assignee {}", taskId,
            assignerAccessControlResponse.getUserInfo().getUid(),
            assigneeAccessControlResponse.map(AccessControlResponse::getUserInfo).map(UserInfo::getUid));

        taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        );
        return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
    }

    @Operation(description = "Completes a Task identified by an id.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "204", description = "Task has been completed", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/complete")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> completeTask(@Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
                                             @Parameter(hidden = true)
                                                @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                             @PathVariable(TASK_ID) String taskId,
                                             @RequestParam(name = REQ_PARAM_COMPLETION_PROCESS, required = false)
                                             String completionProcess,
                                             @RequestBody(required = false) CompleteTaskRequest completeTaskRequest) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Complete task request for task-id {}, user {}", taskId,
                 accessControlResponse.getUserInfo().getUid());

        String validatedCompletionProcess =
            completionProcessValidator.validate(completionProcess, taskId, accessControlResponse).orElse(null);

        if (completeTaskRequest == null || completeTaskRequest.getCompletionOptions() == null) {
            taskManagementService.completeTask(taskId, accessControlResponse, validatedCompletionProcess);
        } else {
            boolean isPrivilegedRequest =
                clientAccessControlService.hasPrivilegedAccess(serviceAuthToken, accessControlResponse);

            if (isPrivilegedRequest) {
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    completeTaskRequest.getCompletionOptions(),
                    validatedCompletionProcess
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

    @Operation(description = "Cancel a Task identified by an id.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "204", description = "Task has been cancelled", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/cancel")
    public ResponseEntity<Void> cancelTask(@Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
                                           @PathVariable(TASK_ID) String taskId,
                                           @RequestParam(name = REQ_PARAM_CANCELLATION_PROCESS, required = false)
                                               String cancellationProcess) {
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Cancel task request for task-id {}, user {}", taskId,
            accessControlResponse.getUserInfo().getUid());
        String validatedCancellationProcess =
            cancellationProcessValidator.validate(cancellationProcess, taskId, accessControlResponse).orElse(null);


        taskManagementService.cancelTask(taskId, accessControlResponse, validatedCancellationProcess);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @Operation(description = "Update Task with notes")
    @ApiResponse(responseCode = "204", description = "Updated Task with notes", content = {
        @Content(mediaType = APPLICATION_JSON_VALUE, schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST, content = {
        @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "404", description = NOT_FOUND, content = {
        @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/notes")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> updatesTaskWithNotes(
        @Parameter(hidden = true) @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
        @PathVariable(TASK_ID) String taskId,
        @RequestBody NotesRequest notesRequest
    ) {

        LOG.info("Task Action: Add task notes request for task-id {}", taskId);
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

    @Operation(description = "Retrieve the role permissions information for the task identified by the given task-id.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "200", description = OK, content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = GetTaskRolePermissionsResponse.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @GetMapping(path = "/{task-id}/roles", consumes = {ALL_VALUE})
    public ResponseEntity<GetTaskRolePermissionsResponse> getTaskRolePermissions(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken, @PathVariable(TASK_ID) String id) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        LOG.info("Task Action: Get task role permission request for task-id {}, user {}", id,
            accessControlResponse.getUserInfo().getUid());

        final List<TaskRolePermissions> taskRolePermissions = taskManagementService.getTaskRolePermissions(
            id,
            accessControlResponse
        );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskRolePermissionsResponse(taskRolePermissions));
    }


    @Operation(description = "Deletes all tasks related to a case.")
    @ApiResponse(responseCode = "201", description = CREATED)
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @PostMapping(path = "/delete", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteTasks(
            @RequestBody final DeleteTasksRequest deleteTasksRequest,
            @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken) {
        try {
            boolean hasAccess = clientAccessControlService.hasPrivilegedAccess(serviceAuthToken);

            if (!hasAccess) {
                return buildErrorResponseEntityAndLogError(HttpStatus.FORBIDDEN.value(),
                        new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR));
            }

            verifyCaseId(deleteTasksRequest.getDeleteCaseTasksAction().getCaseRef());

            taskDeletionService.deleteTasksByCaseId(deleteTasksRequest.getDeleteCaseTasksAction().getCaseRef());

            return status(HttpStatus.CREATED.value())
                    .cacheControl(CacheControl.noCache())
                    .build();

        } catch (final InvalidRequestException invalidRequestException) {
            return buildErrorResponseEntityAndLogError(HttpStatus.BAD_REQUEST.value(), invalidRequestException);
        } catch (final Exception exception) {
            return buildErrorResponseEntityAndLogError(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception);
        }
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
        AssignTaskRequest assignTaskRequest) {

        return assignTaskRequest.getUserId() == null
            ? Optional.empty()
            : Optional.ofNullable(accessControlService.getRolesGivenUserId(
            assignTaskRequest.getUserId(),
            assignerAuthToken
        ));
    }
}
