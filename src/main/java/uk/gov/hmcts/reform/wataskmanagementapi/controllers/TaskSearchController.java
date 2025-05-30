package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequestMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@Validated
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TaskSearchController extends BaseController {

    private static final Logger LOG = getLogger(TaskSearchController.class);
    private final AccessControlService accessControlService;
    private final CftQueryService cftQueryService;
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Value("${config.search.defaultMaxResults}")
    private int defaultMaxResults;


    @Autowired
    public TaskSearchController(AccessControlService accessControlService,
                                CftQueryService cftQueryService,
                                CFTTaskDatabaseService cftTaskDatabaseService,
                                LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider
    ) {
        super();
        this.accessControlService = accessControlService;
        this.cftQueryService = cftQueryService;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
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
    @PostMapping
    public ResponseEntity<GetTasksResponse<Task>> searchWithCriteria(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,

        @RequestParam(required = false, name = "first_result")
        @Min(value = 0, message = "first_result must not be less than zero") Integer firstResult,
        @RequestParam(required = false, name = "max_results")
        @Min(value = 1, message = "max_results must not be less than one") Integer maxResults,
        @Valid @RequestBody SearchTaskRequest searchTaskRequest
    ) {
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

        log.info("Search request received '{}', first_result '{}', max_result '{}'", searchTaskRequest,
            firstResult, maxResults);

        boolean isIndexSearchEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_TASK_SEARCH_GIN_INDEX,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);
        log.info("Search request mapped to '{}', first_result '{}', max_result '{}'", searchRequest,
            Optional.ofNullable(firstResult).orElse(0),
            Optional.ofNullable(maxResults).orElse(defaultMaxResults));

        if (isIndexSearchEnabled) {
            log.info("Search tasks using search_index");
            response = cftTaskDatabaseService.searchForTasks(
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
                searchRequest,
                accessControlResponse);
        } else {
            log.info("Search tasks using Hibernate Queries");
            response = cftQueryService.searchForTasks(
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
                searchRequest,
                accessControlResponse
            );

        }
        boolean isCompletionProcessUpdateEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_COMPLETION_PROCESS_UPDATE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );
        if (!isCompletionProcessUpdateEnabled && response != null && response.getTasks() != null) {
            response.getTasks().forEach(task -> task.setTerminationProcess(null));
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }


    @Operation(description = "Retrieve a list of Task resources identified by set of search"
        + " criteria that are eligible for automatic completion",
        security = {@SecurityRequirement(name = SERVICE_AUTHORIZATION), @SecurityRequirement(name = AUTHORIZATION)})
    @ApiResponse(responseCode = "200", description = OK, content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = GetTasksCompletableResponse.class))})
    @ApiResponse(responseCode = "401", description = UNAUTHORIZED)
    @ApiResponse(responseCode = "403", description = FORBIDDEN)
    @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE)
    @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    @PostMapping(path = "/search-for-completable")
    public ResponseEntity<GetTasksCompletableResponse<Task>> searchWithCriteriaForAutomaticCompletion(
        @Parameter(hidden = true) @RequestHeader(AUTHORIZATION) String authToken,
        @RequestBody SearchEventAndCase searchEventAndCase) {

        log.info("POST /search-for-completable - {}", searchEventAndCase.toString());

        GetTasksCompletableResponse<Task> response;
        Optional<AccessControlResponse> optionalAccessControlResponse = accessControlService
            .getAccessControlResponse(authToken);

        if (optionalAccessControlResponse.isEmpty()) {
            LOG.warn("No role assignments found");
            response = new GetTasksCompletableResponse<>(false, emptyList());
            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
        }

        AccessControlResponse accessControlResponse = optionalAccessControlResponse.get();

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);

        response = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired
        );

        log.info(String.format("POST /search-for-completable - userId: %s, caseId: %s, taskIds: %s",
                               accessControlResponse.getUserInfo().getUid(),
                               searchEventAndCase.getCaseId(),
                               response.getTasks().stream().map(Task::getId).collect(Collectors.toSet())
        ));

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }

}
