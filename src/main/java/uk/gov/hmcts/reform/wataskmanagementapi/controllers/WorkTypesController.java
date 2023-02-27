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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.WorkTypesService;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
@RequestMapping(path = "/work-types", produces = APPLICATION_JSON_VALUE)
@RestController
public class WorkTypesController extends BaseController {
    private final AccessControlService accessControlService;
    private final WorkTypesService workTypesService;

    public WorkTypesController(AccessControlService accessControlService,
                               WorkTypesService workTypesService) {
        super();
        this.accessControlService = accessControlService;
        this.workTypesService = workTypesService;
    }

    @Operation(description = "Retrieve a list of work types with or without filter by user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK, content = {@Content(mediaType = "application/json",
                schema = @Schema(implementation = GetWorkTypesResponse.class))}),
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
    public ResponseEntity<GetWorkTypesResponse> getWorkTypes(
        @RequestHeader(AUTHORIZATION) String authToken,
        @RequestParam(
            required = false, name = "filter-by-user", defaultValue = "false") boolean filterByUser
    ) {
        AccessControlResponse roles = accessControlService.getRoles(authToken);
        List<WorkType> workTypes;

        if (filterByUser) {
            workTypes = workTypesService.getWorkTypes(roles);
        } else {
            workTypes = workTypesService.getAllWorkTypes();
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetWorkTypesResponse(workTypes));
    }
}
