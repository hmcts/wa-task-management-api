package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamService;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@RequestMapping(
    path = "/task",
    consumes = APPLICATION_JSON_VALUE,
    produces = APPLICATION_JSON_VALUE
)
@RestController
public class TaskController {

    private final CamundaService camundaService;
    private final IdamService idamService;

    @Autowired
    public TaskController(CamundaService camundaService, IdamService idamService) {
        this.camundaService = camundaService;
        this.idamService = idamService;
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
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetTasksResponse<Task>> searchWithCriteria(@RequestBody SearchTaskRequest searchTaskRequest) {

        if (searchTaskRequest.getSearchParameters() == null || searchTaskRequest.getSearchParameters().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = camundaService.searchWithCriteria(searchTaskRequest);
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
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @GetMapping(path = "/{task-id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<GetTaskResponse<CamundaTask>> getTask(@PathVariable("task-id") String id) {
        CamundaTask task = camundaService.getTask(id);
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
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @PostMapping(path = "/{task-id}/claim",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> claimTask(@RequestHeader("Authorization") String authToken,
                                            @PathVariable("task-id") String taskId) {
        String userId = idamService.getUserId(authToken);
        camundaService.claimTask(taskId, userId);
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
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @PostMapping(path = "/{task-id}/unclaim",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unclaimTask(@RequestHeader("Authorization") String authToken,
                                              @PathVariable("task-id") String taskId) {
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
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<String> assignTask(@RequestHeader("Authorization") String authToken,
                                             @PathVariable("task-id") String taskId) {

        String userId = idamService.getUserId(authToken);
        camundaService.assignTask(taskId, userId);
        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }

    @ApiOperation("Completes a Task identified by an id.")
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Task has been completed"
        ),
        @ApiResponse(
            code = 400,
            message = "Bad Request"
        ),
        @ApiResponse(
            code = 403,
            message = "Forbidden"
        ),
        @ApiResponse(
            code = 415,
            message = "Unsupported Media Type"
        ),
        @ApiResponse(
            code = 500,
            message = "Internal Server Error"
        )
    })
    @PostMapping(path = "/{task-id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable("task-id") String taskId) {
        camundaService.completeTask(taskId);

        return ResponseEntity
            .noContent()
            .cacheControl(CacheControl.noCache())
            .build();
    }
}
