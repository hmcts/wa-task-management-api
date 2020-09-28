package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.util.Objects;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.UnusedPrivateField",
    "PMD.SingularField"
})
@RequestMapping(
    path = "/task",
    consumes = APPLICATION_JSON_VALUE,
    produces = APPLICATION_JSON_VALUE
)
@RestController
public class TaskController {

    private final CamundaService camundaService;

    @Autowired
    public TaskController(CamundaService camundaService) {
        this.camundaService = camundaService;
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
    public ResponseEntity<GetTasksResponse<CamundaTask>> searchWithCriteria() {
        throw new NotImplementedException();
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
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(new GetTaskResponse<>(new CamundaTask(id)));
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
    public ResponseEntity<String> claimTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }

    @ApiOperation("Unclaim the identified Task for the currently logged in user.")
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
    @PostMapping(path = "/{task-id}/unclaim",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unclaimTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }

    @ApiOperation("Assign the identified Task to a specified user.")
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
    @PostMapping(path = "/{task-id}/assign")
    public ResponseEntity<String> assignTask(@PathVariable("task-id") String taskId,
                                             @RequestBody AssignTaskRequest assignTaskRequest) {

        Objects.requireNonNull(assignTaskRequest);
        Objects.requireNonNull(assignTaskRequest.getUserId());

        throw new NotImplementedException();
    }

    @ApiOperation("Completes a Task identified by an id.")
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
    @PostMapping(path = "/{task-id}/complete",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> completeTask(@PathVariable("task-id") String taskId) {
        throw new NotImplementedException();
    }
}
