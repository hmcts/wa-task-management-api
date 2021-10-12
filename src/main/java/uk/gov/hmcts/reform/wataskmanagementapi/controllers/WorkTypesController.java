package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
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

    @ApiOperation("Retrieve a list of work types for the current user")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetWorkTypesResponse.class),
        @ApiResponse(code = 502, message = "Downstream Dependency Error", response = String.class),
        @ApiResponse(code = 503, message = "Service Unavailable", response = String.class)
    })
    @GetMapping(path = "/users")
    public ResponseEntity<GetWorkTypesResponse> getWorkTypes(@RequestHeader(AUTHORIZATION) String authToken) {

        AccessControlResponse roles = accessControlService.getRoles(authToken);
        List<WorkType> workTypes = workTypesService.getWorkTypes(roles);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetWorkTypesResponse(workTypes));

    }

}
