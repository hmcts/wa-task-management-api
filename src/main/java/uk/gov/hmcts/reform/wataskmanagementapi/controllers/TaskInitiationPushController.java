package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskInitiationPushService;

import java.time.ZonedDateTime;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@Slf4j
public class TaskInitiationPushController extends BaseController {

    private final ClientAccessControlService clientAccessControlService;
    private final TaskInitiationPushService taskInitiationPushService;

    public TaskInitiationPushController(ClientAccessControlService clientAccessControlService,
                                        TaskInitiationPushService taskInitiationPushService) {
        this.clientAccessControlService = clientAccessControlService;
        this.taskInitiationPushService = taskInitiationPushService;
    }

    @Operation(description = "Exclusive access only: initiate a task directly from a Camunda push payload.")
    @ApiResponse(responseCode = "201", description = "Task has been initiated", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResource.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/{task-id}/initiation-push")
    public ResponseEntity<TaskResource> initiate(@Parameter(hidden = true)
                                                     @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                                 @PathVariable(TASK_ID) String taskId,
                                                 @RequestBody CamundaTaskInitiationRequest request) {
        if (!clientAccessControlService.hasExclusiveAccess(serviceAuthToken)) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        log.info("lars iniate task new endpoint, {}, {}", taskId, ZonedDateTime.now());
        log.info("Push initiate task-id {} for processInstanceId {}", taskId, request.getProcessInstanceId());

        TaskResource savedTask = taskInitiationPushService.initiateTask(taskId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }
}
