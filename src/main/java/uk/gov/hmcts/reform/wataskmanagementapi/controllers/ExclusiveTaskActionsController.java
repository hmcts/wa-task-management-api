package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
public class ExclusiveTaskActionsController extends BaseController {

    private final TaskManagementService taskManagementService;
    private final ClientAccessControlService clientAccessControlService;

    @Autowired
    public ExclusiveTaskActionsController(ClientAccessControlService clientAccessControlService,
                                          TaskManagementService taskManagementService) {
        super();
        this.clientAccessControlService = clientAccessControlService;
        this.taskManagementService = taskManagementService;
    }

    @ApiOperation("Exclusive access only: Initiate a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Task has been initiated", response = TaskResource.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/{task-id}")
    public ResponseEntity<TaskResource> initiate(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                                 @PathVariable(TASK_ID) String taskId,
                                                 @RequestBody InitiateTaskRequest initiateTaskRequest) {

        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthToken);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        TaskResource savedTask = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }


    @ApiOperation("Exclusive access only: Terminate a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(code = 204, message = "Task has been terminated", response = Object.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{task-id}")
    public ResponseEntity<Void> terminateTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                              @PathVariable(TASK_ID) String taskId,
                                              @RequestBody TerminateTaskRequest terminateTaskRequest) {

        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthToken);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        taskManagementService.terminateTask(taskId, terminateTaskRequest.getTerminateInfo());

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

}
