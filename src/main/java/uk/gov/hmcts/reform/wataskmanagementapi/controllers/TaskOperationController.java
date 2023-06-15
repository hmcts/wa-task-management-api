package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.CyclomaticComplexity", "PMD.AvoidDuplicateLiterals"})
public class TaskOperationController extends BaseController {

    private final TaskOperationService taskOperationService;
    private final ClientAccessControlService clientAccessControlService;

    @Autowired
    public TaskOperationController(TaskOperationService taskOperationService,
                                   ClientAccessControlService clientAccessControlService) {
        super();
        this.taskOperationService = taskOperationService;
        this.clientAccessControlService = clientAccessControlService;
    }

    @Operation(description = "performs specified operation like marking tasks to reconfigure and execute reconfigure.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Task operation has been completed"),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(path = "/operation")
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public ResponseEntity<Void> performOperation(@Parameter(hidden = true)
                                                     @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                                 @RequestBody TaskOperationRequest taskOperationRequest) {
        log.info("Task operation request received '{}'", taskOperationRequest);
        boolean hasExclusiveAccessRequest =
            clientAccessControlService.hasExclusiveAccess(serviceAuthToken);

        if (hasExclusiveAccessRequest) {
            taskOperationService.performOperation(
                taskOperationRequest
            );
        } else {
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

}
