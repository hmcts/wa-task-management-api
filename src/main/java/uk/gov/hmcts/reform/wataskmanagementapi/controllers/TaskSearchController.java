package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.SearchTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
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

    @ApiOperation("Retrieve a list of Task resources identified by set of search criteria.")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetTasksResponse.class),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @PostMapping
    public ResponseEntity<GetTasksResponse<Task>> searchWithCriteria(
        @RequestHeader("Authorization") String authToken,
        @RequestParam(required = false, name = "first_result") Optional<Integer> firstResult,
        @RequestParam(required = false, name = "max_results") Optional<Integer> maxResults,
        @Valid @RequestBody SearchTaskRequest searchTaskRequest
    ) {

        //Safe-guard
        if (searchTaskRequest.getSearchParameters() == null || searchTaskRequest.getSearchParameters().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        boolean isFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );
        if (isFeatureEnabled) {
            //Release 2
            List<PermissionTypes> permissionsRequired = singletonList(READ);
            GetTasksResponse<Task> tasksResponse = cftQueryService.searchForTasks(
                firstResult.orElse(0),
                maxResults.orElse(defaultMaxResults),
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            );

            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(tasksResponse);
        } else {
            //Release 1
            List<Task> tasks = taskManagementService.searchWithCriteria(
                searchTaskRequest, firstResult.orElse(0), maxResults.orElse(defaultMaxResults),
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


    @ApiOperation("Retrieve a list of Task resources identified by set of search"
                  + " criteria that are eligible for automatic completion")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK, response = GetTasksCompletableResponse.class),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @PostMapping(path = "/search-for-completable")
    public ResponseEntity<GetTasksCompletableResponse<Task>> searchWithCriteriaForAutomaticCompletion(
        @RequestHeader("Authorization") String authToken,
        @RequestBody SearchEventAndCase searchEventAndCase) {

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        boolean isFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_TASK_QUERY,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        GetTasksCompletableResponse<Task> response;
        if (isFeatureEnabled) {
            List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
            response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase, accessControlResponse, permissionsRequired);
        }  else {
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

    @ExceptionHandler(NoRoleAssignmentsFoundException.class)
    public ResponseEntity<SearchTasksResponse> handleNoRoleAssignmentsException(Exception ex) {
        LOG.warn("No role assignments found");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(new SearchTasksResponse(Collections.emptyList()));
    }
}