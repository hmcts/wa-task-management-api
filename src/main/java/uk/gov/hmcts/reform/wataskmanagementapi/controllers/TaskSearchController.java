package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import javax.validation.Valid;
import javax.validation.constraints.Min;

import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

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

    @Operation(description = "Retrieve a list of Task resources identified by set of search criteria.")
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
        @RequestHeader("Authorization") String authToken,

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

        log.debug("Search request received '{}'", searchTaskRequest);
        //Release 2

        boolean granularPermissionResponseFeature = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_4_GRANULAR_PERMISSION_RESPONSE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        boolean isGranularPermissionEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        boolean isIndexSearchEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_TASK_SEARCH_GIN_INDEX,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(searchTaskRequest);

        if (isIndexSearchEnabled) {
            response = cftTaskDatabaseService.searchForTasks(
                searchRequest,
                accessControlResponse,
                granularPermissionResponseFeature);
        } else {
            response = cftQueryService.searchForTasks(
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
                searchRequest,
                accessControlResponse,
                granularPermissionResponseFeature,
                isGranularPermissionEnabled
            );
        }

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }


    @Operation(description = "Retrieve a list of Task resources identified by set of search"
        + " criteria that are eligible for automatic completion")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK, content = {@Content(mediaType = "application/json",
                schema = @Schema(implementation = GetTasksCompletableResponse.class))}),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @PostMapping(path = "/search-for-completable")
    public ResponseEntity<GetTasksCompletableResponse<Task>> searchWithCriteriaForAutomaticCompletion(
        @RequestHeader("Authorization") String authToken,
        @RequestBody SearchEventAndCase searchEventAndCase) {

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

        boolean granularPermissionResponseFeature = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_4_GRANULAR_PERMISSION_RESPONSE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        response = cftQueryService.searchForCompletableTasks(
            searchEventAndCase,
            accessControlResponse.getRoleAssignments(),
            permissionsRequired,
            granularPermissionResponseFeature
        );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }

}
