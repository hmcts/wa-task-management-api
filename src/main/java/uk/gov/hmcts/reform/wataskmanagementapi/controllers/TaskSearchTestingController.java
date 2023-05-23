package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequestMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.GENERIC_FORBIDDEN_ERROR;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@Validated
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TaskSearchTestingController extends BaseController {

    private static final Logger LOG = getLogger(TaskSearchTestingController.class);
    private final AccessControlService accessControlService;
    private final CftQueryService cftQueryService;

    @Value("${config.search.defaultMaxResults}")
    private int defaultMaxResults;

    @Value("${environment}")
    private String environment;


    @Autowired
    public TaskSearchTestingController(AccessControlService accessControlService,
                                       CftQueryService cftQueryService
    ) {
        super();
        this.accessControlService = accessControlService;
        this.cftQueryService = cftQueryService;
    }

    @Operation(description = "Retrieve a list of Task resources identified by set of search criteria.",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = OK, content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = GetTasksResponse.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @PostMapping(path = "/extended-search")
    public ResponseEntity<GetTasksResponse<Task>> searchWithCriteria(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,

        @RequestParam(required = false, name = "first_result")
        @Min(value = 0, message = "first_result must not be less than zero") Integer firstResult,
        @RequestParam(required = false, name = "max_results")
        @Min(value = 1, message = "max_results must not be less than one") Integer maxResults,
        @Valid @RequestBody SearchTaskRequest searchTaskRequest
    ) {

        if (isProdEnvironment()) {
            log.error("Task extended search endpoint not allowed in '{}' environment.", environment);
            throw new GenericForbiddenException(GENERIC_FORBIDDEN_ERROR);
        }

        log.info("Search request received '{}'", searchTaskRequest);
        //Safe-guard
        if (searchTaskRequest.getSearchParameters() == null || searchTaskRequest.getSearchParameters().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }


        GetTasksResponse<Task> response;
        Optional<AccessControlResponse> optionalAccessControlResponse = accessControlService
            .getAccessControlResponse(authToken);
        if (optionalAccessControlResponse.isEmpty()) {
            LOG.warn("No role assignments found");
            response = new GetTasksResponse<>(emptyList(), 0);
            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
        }

        AccessControlResponse accessControlResponse = optionalAccessControlResponse.get();
        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        log.info("Search request mapped to '{}'", searchRequest);

        response = cftQueryService.searchForTasks(
            Optional.ofNullable(firstResult).orElse(0),
            Optional.ofNullable(maxResults).orElse(defaultMaxResults),
            searchRequest,
            accessControlResponse
        );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }

    private boolean isProdEnvironment() {
        log.info("Task extended search endpoint accessing in '{}' environment ", environment);
        return "prod".equalsIgnoreCase(environment);
    }
}
