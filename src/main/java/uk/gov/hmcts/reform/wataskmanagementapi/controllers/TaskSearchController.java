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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@Validated
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TaskSearchController extends BaseController {

    private static final Logger LOG = getLogger(TaskSearchController.class);
    private final TaskManagementService taskManagementService;
    private final AccessControlService accessControlService;
    private final CftQueryService cftQueryService;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Value("${config.search.defaultMaxResults}")
    private int defaultMaxResults;


    @Autowired
    public TaskSearchController(TaskManagementService taskManagementService,
                                AccessControlService accessControlService,
                                CftQueryService cftQueryService,
                                LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider
    ) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
        this.cftQueryService = cftQueryService;
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

        boolean isFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        if (isFeatureEnabled) {
            log.debug("Search request received '{}'", searchTaskRequest);
            //Release 2
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(READ);
            response = cftQueryService.searchForTasks(
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
                searchTaskRequest,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            );

            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
        } else {
            //Release 1
            List<Task> tasks = taskManagementService.searchWithCriteria(
                searchTaskRequest,
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
                accessControlResponse
            );

            if (tasks.isEmpty()) {
                return ResponseEntity
                    .ok()
                    .cacheControl(CacheControl.noCache())
                    .body(new GetTasksResponse<>(tasks, 0));
            } else {
                final long taskCount = taskManagementService.getTaskCount(searchTaskRequest);
                return ResponseEntity
                    .ok()
                    .cacheControl(CacheControl.noCache())
                    .body(new GetTasksResponse<>(tasks, taskCount));
            }
        }
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

        boolean isFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        if (isFeatureEnabled) {
            List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
            response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            );
        } else {
            response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );
        }
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }

}
