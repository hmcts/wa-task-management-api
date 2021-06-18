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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.privilege.PrivilegedAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
public class TaskActionsController {
    private static final Logger LOG = getLogger(TaskActionsController.class);

    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String FORBIDDEN = "Forbidden";
    private static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String TASK_ID = "task-id";
    private final CamundaService camundaService;
    private final AccessControlService accessControlService;
    private final PrivilegedAccessControlService privilegedAccessControlService;
    private final SystemDateProvider systemDateProvider;

    @Autowired
    public TaskActionsController(CamundaService camundaService,
                                 AccessControlService accessControlService,
                                 SystemDateProvider systemDateProvider,
                                 PrivilegedAccessControlService privilegedAccessControlService
    ) {
        this.camundaService = camundaService;
        this.accessControlService = accessControlService;
        this.systemDateProvider = systemDateProvider;
        this.privilegedAccessControlService = privilegedAccessControlService;
    }

    @ApiOperation("Retrieve a Task Resource identified by its unique id.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = GetTaskResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @GetMapping(path = "/{task-id}")
    public ResponseEntity<GetTaskResponse<Task>> getTask(@RequestHeader("Authorization") String authToken,
                                                         @PathVariable(TASK_ID) String id) {

        List<PermissionTypes> endpointPermissionsRequired = singletonList(READ);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        Task task = camundaService.getTask(id, accessControlResponse.getRoleAssignments(), endpointPermissionsRequired);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskResponse<>(task));
    }

    @ApiOperation("Claim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "No Content", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}/claim")
    public ResponseEntity<Void> claimTask(@RequestHeader("Authorization") String authToken,
                                          @PathVariable(TASK_ID) String taskId) {

        List<PermissionTypes> endpointPermissionsRequired = asList(OWN, EXECUTE);

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        camundaService.claimTask(taskId, accessControlResponse, endpointPermissionsRequired);
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
    public ResponseEntity<Void> unclaimTask(@RequestHeader("Authorization") String authToken,
                                            @PathVariable(TASK_ID) String taskId) {

        List<PermissionTypes> endpointPermissionsRequired = singletonList(MANAGE);

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        camundaService.unclaimTask(taskId, accessControlResponse, endpointPermissionsRequired);
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
    public ResponseEntity<Void> assignTask(@RequestHeader("Authorization") String assignerAuthToken,
                                           @PathVariable(TASK_ID) String taskId,
                                           @RequestBody AssignTaskRequest assignTaskRequest) {

        List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
        List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

        AccessControlResponse assignerAccessControlResponse = accessControlService.getRoles(assignerAuthToken);
        AccessControlResponse assigneeAccessControlResponse = accessControlService.getRolesGivenUserId(
            assignTaskRequest.getUserId(),
            assignerAuthToken
        );

        camundaService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assignerPermissionsRequired,
            assigneeAccessControlResponse,
            assigneePermissionsRequired
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
        List<PermissionTypes> endpointPermissionsRequired = asList(OWN, EXECUTE);

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        if (completeTaskRequest == null || completeTaskRequest.getCompletionOptions() == null) {
            camundaService.completeTask(taskId, accessControlResponse, endpointPermissionsRequired);
        } else {
            boolean isPrivilegedRequest =
                privilegedAccessControlService.hasPrivilegedAccess(serviceAuthToken, accessControlResponse);

            if (isPrivilegedRequest) {
                camundaService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    endpointPermissionsRequired,
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
    public ResponseEntity<Void> cancelTask(@RequestHeader("Authorization") String authToken,
                                           @PathVariable(TASK_ID) String taskId) {
        List<PermissionTypes> endpointPermissionsRequired = singletonList(CANCEL);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        camundaService.cancelTask(taskId, accessControlResponse, endpointPermissionsRequired);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @ExceptionHandler(NoRoleAssignmentsFoundException.class)
    public ResponseEntity<ErrorMessage> handleNoRoleAssignmentsException(Exception ex) {
        LOG.warn("No role assignments found");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .cacheControl(CacheControl.noCache())
            .body(new ErrorMessage(
                ex,
                HttpStatus.UNAUTHORIZED,
                systemDateProvider.nowWithTime()
            ));
    }

}
