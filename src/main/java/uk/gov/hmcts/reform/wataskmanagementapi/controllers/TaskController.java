package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.PermissionCheckService;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports"})
@RequestMapping(
    path = "/task",
    consumes = APPLICATION_JSON_VALUE,
    produces = APPLICATION_JSON_VALUE
)
@RestController
public class TaskController {

    private static final String BAD_REQUEST = "Bad Request";
    private static final String FORBIDDEN = "Forbidden";
    private static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String TASK_ID = "task-id";

    private final CamundaService camundaService;
    private final AccessControlService accessControlService;
    private final PermissionCheckService permissionCheckService;

    @Autowired
    public TaskController(CamundaService camundaService,
                          AccessControlService accessControlService,
                          PermissionCheckService permissionCheckService) {
        this.camundaService = camundaService;
        this.accessControlService = accessControlService;
        this.permissionCheckService = permissionCheckService;
    }

    @ApiOperation("Retrieve a list of Task resources identified by set of search criteria.")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "OK",
            response = GetTasksResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetTasksResponse<Task>> searchWithCriteria(@RequestHeader("Authorization") String authToken,
                                                                     @RequestBody SearchTaskRequest searchTaskRequest) {
        //Safe-guard
        if (searchTaskRequest.getSearchParameters() == null || searchTaskRequest.getSearchParameters().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<PermissionTypes> endpointPermissionsRequired = singletonList(READ);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        List<Task> tasks = camundaService.searchWithCriteria(
            searchTaskRequest,
            accessControlResponse.getRoleAssignments(),
            endpointPermissionsRequired
        );
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTasksResponse<>(tasks));
    }

    @ApiOperation("Retrieve a Task Resource identified by its unique id.")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "OK",
            response = GetTaskResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @GetMapping(path = "/{task-id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetTaskResponse<Task>> getTask(@RequestHeader("Authorization") String authToken,
                                                         @PathVariable(TASK_ID) String id) {

        List<PermissionTypes> endpointPermissionsRequired = singletonList(READ);
        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        Task task = camundaService.getTask(id, accessControlResponse.getRoleAssignments(), endpointPermissionsRequired);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskResponse<>(task));
    }

    @ApiOperation("Claim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "No Content"
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @PostMapping(path = "/{task-id}/claim",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> claimTask(@RequestHeader("Authorization") String authToken,
                                            @PathVariable(TASK_ID) String taskId) {

        List<PermissionTypes> endpointPermissionsRequired = asList(OWN, EXECUTE);

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);
        camundaService.claimTask(taskId, accessControlResponse, endpointPermissionsRequired);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();

    }

    @ApiOperation("Unclaim the identified Task for the currently logged in user.")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Task unclaimed"
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @PostMapping(path = "/{task-id}/unclaim",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unclaimTask(@RequestHeader("Authorization") String authToken,
                                              @PathVariable(TASK_ID) String taskId) {
        camundaService.unclaimTask(taskId);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @ApiOperation("Assign the identified Task to a specified user.")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Task assigned"
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<String> assignTask(@RequestHeader("Authorization") String authToken,
                                             @PathVariable(TASK_ID) String taskId,
                                             @RequestBody AssigneeRequest assigneeRequest) {
        boolean permissionCheckResult = permissionCheckService.validate(authToken, taskId, assigneeRequest.getUserId());

        if (permissionCheckResult) {
            camundaService.assignTask(taskId, assigneeRequest.getUserId());
            return ResponseEntity.noContent().cacheControl(CacheControl.noCache()).build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @ApiOperation("Completes a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Task has been completed"
        ),
        @ApiResponse(
            code = 400,
            message = BAD_REQUEST
        ),
        @ApiResponse(
            code = 403,
            message = FORBIDDEN
        ),
        @ApiResponse(
            code = 415,
            message = UNSUPPORTED_MEDIA_TYPE
        ),
        @ApiResponse(
            code = 500,
            message = INTERNAL_SERVER_ERROR
        )
    })
    @PostMapping(path = "/{task-id}/complete")
    public ResponseEntity<Void> completeTask(@RequestHeader("Authorization") String authToken,
                                             @PathVariable(TASK_ID) String taskId) {
        List<PermissionTypes> endpointPermissionsRequired = asList(OWN, EXECUTE);

        AccessControlResponse accessControlResponse = accessControlService.getRoles(authToken);

        camundaService.completeTask(taskId, accessControlResponse, endpointPermissionsRequired);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }
}
