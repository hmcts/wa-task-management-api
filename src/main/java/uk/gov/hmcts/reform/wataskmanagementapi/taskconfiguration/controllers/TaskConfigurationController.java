package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions.ConfigureTaskException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RequestMapping(path = "/task", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
public class TaskConfigurationController {
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String OK = "OK";
    private static final String BAD_REQUEST = "Bad Request";
    private static final String FORBIDDEN = "Forbidden";
    private static final String UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type";
    private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    private static final String TASK_ID = "task-id";
    private final ConfigureTaskService configureTaskService;

    public TaskConfigurationController(ConfigureTaskService configureTaskService) {
        this.configureTaskService = configureTaskService;
    }

    @ApiOperation("Given an existent task id configures a task over rest")
    @ApiResponses({
        @ApiResponse(code = 200, message = OK),
        @ApiResponse(code = 400, message = BAD_REQUEST),
        @ApiResponse(code = 403, message = FORBIDDEN),
        @ApiResponse(code = 401, message = UNAUTHORIZED),
        @ApiResponse(code = 415, message = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)
    })
    @PostMapping(path = "/{task-id}")
    public ResponseEntity<String> configureTask(
        @PathVariable(TASK_ID) String taskId) {
        try {
            configureTaskService.configureTask(taskId);
            return ResponseEntity
                .ok()
                .body("OK");
        } catch (ConfigureTaskException exc) {
            return ResponseEntity.notFound().build();
        }
    }


}
