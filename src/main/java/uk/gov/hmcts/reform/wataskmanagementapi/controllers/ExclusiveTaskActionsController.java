package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@Slf4j
public class ExclusiveTaskActionsController extends BaseController {

    private final TaskManagementService taskManagementService;
    private final ClientAccessControlService clientAccessControlService;
    @Value("${config.initiationRequestRequiredFields}")
    private List<String> initiationRequestRequiredFields;

    @Autowired
    public ExclusiveTaskActionsController(ClientAccessControlService clientAccessControlService,
                                          TaskManagementService taskManagementService) {
        super();
        this.clientAccessControlService = clientAccessControlService;
        this.taskManagementService = taskManagementService;
    }

    @Operation(description = "Exclusive access only: Initiate a Task identified by an id.")

    @ApiResponse(responseCode = "201", description = "Task has been initiated", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResource.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "/{task-id}/initiation")
    public ResponseEntity<TaskResource> initiate(@Parameter(hidden = true)
                                                     @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                                 @PathVariable(TASK_ID) String taskId,
                                                 @RequestBody InitiateTaskRequestMap initiateTaskRequest) {
        log.info("Initiate task-id {} with attributes: {} ", taskId, initiateTaskRequest.getTaskAttributes());
        boolean hasAccess = clientAccessControlService.hasExclusiveAccess(serviceAuthToken);
        if (!hasAccess) {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }
        validateInitiationRequestMap(initiateTaskRequest.getTaskAttributes());
        TaskResource savedTask = taskManagementService.initiateTask(taskId, initiateTaskRequest);
        taskManagementService.updateTaskIndex(savedTask.getTaskId());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noCache())
            .body(savedTask);
    }

    private void validateInitiationRequestMap(Map<String, Object> taskAttributes) {
        List<Violation> violations = new ArrayList<>();
        String errorMessage = "must not be empty";
        initiationRequestRequiredFields.forEach(mandatoryField -> {
            Object value = taskAttributes != null ? taskAttributes.get(mandatoryField) : null;

            if (value == null || value.toString().isBlank()) {
                violations.add(new Violation(mandatoryField, errorMessage));
            }
        });

        if (!violations.isEmpty()) {
            throw new CustomConstraintViolationException(violations);
        }
    }

    @Operation(description = "Exclusive access only: Terminate a Task identified by an id.")
    @ApiResponse(responseCode = "204", description = "Task has been terminated", content = {
        @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))})
    @ApiResponse(responseCode = "400", description = BAD_REQUEST)
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(path = "/{task-id}")
    public ResponseEntity<Void> terminateTask(@Parameter(hidden = true)
                                                  @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                              @PathVariable(TASK_ID) String taskId,
                                              @RequestBody TerminateTaskRequest terminateTaskRequest) {
        log.info("Terminate task-id {} with {} ", taskId, terminateTaskRequest.getTerminateInfo());
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
