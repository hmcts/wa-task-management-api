package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.BadRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.Optional;
import javax.validation.Valid;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CyclomaticComplexity"})
public class TaskActionsController extends BaseController {
    private static final Logger LOG = getLogger(TaskActionsController.class);

    private final TaskManagementService taskManagementService;
    private final AccessControlService accessControlService;
    private final ClientAccessControlService clientAccessControlService;
    private final SystemDateProvider systemDateProvider;

    @Autowired
    public TaskActionsController(TaskManagementService taskManagementService,
                                 AccessControlService accessControlService,
                                 SystemDateProvider systemDateProvider,
                                 ClientAccessControlService clientAccessControlService) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
        this.systemDateProvider = systemDateProvider;
        this.clientAccessControlService = clientAccessControlService;
    }

    @ApiOperation("Retrieve a Task Resource identified by its unique id.")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetTaskResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
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

    @ApiOperation("Claim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(code = 204, message = NO_CONTENT, response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
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

    @ApiOperation("Unclaim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Task unclaimed", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
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

    @ApiOperation("Assign the identified Task to a specified user.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Task assigned", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<Void> assignTask(@RequestHeader(AUTHORIZATION) String assignerAuthToken,
                                           @PathVariable(TASK_ID) String taskId,
                                           @RequestBody AssignTaskRequest assignTaskRequest) {

        AccessControlResponse assignerAccessControlResponse = accessControlService.getRoles(assignerAuthToken);
        AccessControlResponse assigneeAccessControlResponse = accessControlService.getRolesGivenUserId(
            assignTaskRequest.getUserId(),
            assignerAuthToken
        );

        taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        );
        return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
    }

    @ApiOperation("Completes a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Task has been completed", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
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

    @ApiOperation("Cancel a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Task has been cancelled", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
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

    @ApiOperation("Update Task with notes")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Updated Task with notes", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 404, message = NOT_FOUND),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/notes")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> updatesTaskWithNotes(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
        @PathVariable(TASK_ID) String taskId,
        @Valid @RequestBody NotesRequest notesRequest
    ) {

        if (notesRequest == null) {
            throw new BadRequestException("Bad Request");
        }

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

}
