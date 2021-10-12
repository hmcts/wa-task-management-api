package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetWorkTypesResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;
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

    @ApiOperation("Retrieve a list of work types with or without filter by user")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetTaskResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR),
        @ApiResponse(code = 502, message = "Downstream Dependency Error", response = String.class),
        @ApiResponse(code = 503, message = "Service Unavailable", response = String.class)
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
