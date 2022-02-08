package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;
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
    private final SystemDateProvider systemDateProvider;

    @Value("${config.search.defaultMaxResults}")
    private int defaultMaxResults;


    @Autowired
    public TaskSearchController(TaskManagementService taskManagementService,
                                AccessControlService accessControlService,
                                CftQueryService cftQueryService,
                                LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                SystemDateProvider systemDateProvider
    ) {
        super();
        this.taskManagementService = taskManagementService;
        this.accessControlService = accessControlService;
        this.cftQueryService = cftQueryService;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.systemDateProvider = systemDateProvider;
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

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
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
            GetTasksResponse<Task> tasksResponse = cftQueryService.searchForTasks(
                Optional.ofNullable(firstResult).orElse(0),
                Optional.ofNullable(maxResults).orElse(defaultMaxResults),
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
                searchEventAndCase,
                accessControlResponse,
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

    @ExceptionHandler(NoRoleAssignmentsFoundException.class)
    public ResponseEntity<ErrorMessage> handleNoRoleAssignmentsException(Exception ex) {
        LOG.warn("No role assignments found");
        return status(HttpStatus.UNAUTHORIZED)
            .cacheControl(CacheControl.noCache())
            .body(new ErrorMessage(
                ex,
                HttpStatus.UNAUTHORIZED,
                systemDateProvider.nowWithTime()
            ));
    }
}
