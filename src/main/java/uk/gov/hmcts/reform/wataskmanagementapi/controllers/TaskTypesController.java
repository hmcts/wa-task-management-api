package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskTypesService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
@RequestMapping(path = "/task/task-types", produces = APPLICATION_JSON_VALUE)
@RestController
public class TaskTypesController extends BaseController {

    private final AccessControlService accessControlService;
    private final TaskTypesService taskTypesService;

    public TaskTypesController(AccessControlService accessControlService,
                               TaskTypesService taskTypesService) {
        super();
        this.accessControlService = accessControlService;
        this.taskTypesService = taskTypesService;
    }

    @Operation(description = "Retrieve list of task types with filter by jurisdiction")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = OK,
            content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = GetTaskTypesResponse.class))
            }),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR),
        @ApiResponse(responseCode = "502", description = "Downstream Dependency Error", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))}),
        @ApiResponse(responseCode = "503", description = "Service Unavailable", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))})
    })
    @GetMapping
    public ResponseEntity<GetTaskTypesResponse> getTaskTypes(
        @RequestHeader(AUTHORIZATION) String authToken,
        @RequestParam(name = "jurisdiction") String jurisdiction
    ) {
        AccessControlResponse roles = accessControlService.getRoles(authToken);
        GetTaskTypesResponse response = taskTypesService.getTaskTypes(roles, jurisdiction);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }
}
