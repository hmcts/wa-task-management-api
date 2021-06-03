package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@Slf4j
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
public class TaskSearchController {

    private static final Logger LOG = getLogger(TaskSearchController.class);
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String FORBIDDEN = "Forbidden";
    private static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private final CamundaService camundaService;
    private final AccessControlService accessControlService;

    @Autowired
    public TaskSearchController(CamundaService camundaService,
                                AccessControlService accessControlService
    ) {
        this.camundaService = camundaService;
        this.accessControlService = accessControlService;
    }

    @ApiOperation("Retrieve a list of Task resources identified by set of search criteria.")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = GetTasksResponse.class),
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
        @RequestBody SearchTaskRequest searchTaskRequest
    ) {
        //Safe-guard
        if (searchTaskRequest.getSearchParameters() == null || searchTaskRequest.getSearchParameters().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<PermissionTypes> endpointPermissionsRequired = singletonList(READ);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        List<Task> tasks = camundaService.searchWithCriteria(
            searchTaskRequest, firstResult.orElse(0), maxResults.orElse(Integer.MAX_VALUE),
            accessControlResponse,
            endpointPermissionsRequired
        );

        if (tasks.isEmpty()) {
            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(new GetTasksResponse<>(tasks, 0));
        } else {
            final long taskCount = camundaService.getTaskCount(searchTaskRequest);
            return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noCache())
                .body(new GetTasksResponse<>(tasks, taskCount));
        }
    }


    @ApiOperation("Retrieve a list of Task resources identified by set of search"
                  + " criteria that are eligible for automatic completion")
    @ApiResponses({
        @ApiResponse(code = 200, message = "OK", response = GetTasksCompletableResponse.class),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 415, message = "Unsupported Media Type"),
        @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @PostMapping(path = "/search-for-completable")
    public ResponseEntity<GetTasksCompletableResponse<Task>> searchWithCriteriaForAutomaticCompletion(
        @RequestHeader("Authorization") String authToken,
        @RequestBody SearchEventAndCase searchEventAndCase) {

        List<PermissionTypes> endpointPermissionsRequired = asList(OWN, EXECUTE);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        List<Task> tasks = camundaService.searchForCompletableTasks(
            searchEventAndCase,
            endpointPermissionsRequired,
            accessControlResponse

        );
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTasksCompletableResponse<>(tasks));
    }

    @ExceptionHandler(NoRoleAssignmentsFoundException.class)
    public ResponseEntity<List<Task>> handleNoRoleAssignmentsException(Exception ex) {
        LOG.warn("No role assignments found");
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(emptyList());
    }
}
