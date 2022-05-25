package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions.ConfigureTaskException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;

@Slf4j
@RequestMapping(path = "/task-configuration", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
@RestController
@SuppressWarnings("PMD.LawOfDemeter")
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

    @Operation(description = "Given an existent task id configures a task over rest")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
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

    @Operation(description = "Retrieves a list of configuration variables to be added to a task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = OK,  content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigureTaskResponse.class))}),
        @ApiResponse(responseCode = "400", description = BAD_REQUEST),
        @ApiResponse(responseCode = "403", description = FORBIDDEN),
        @ApiResponse(responseCode = "401", description = UNAUTHORIZED),
        @ApiResponse(responseCode = "415", description = UNSUPPORTED_MEDIA_TYPE),
        @ApiResponse(responseCode = "500", description = INTERNAL_SERVER_ERROR)
    })
    @PostMapping(path = "/{task-id}/configuration")
    public ResponseEntity<ConfigureTaskResponse> getConfigurationForTask(
        @PathVariable(TASK_ID) String taskId,
        @RequestBody ConfigureTaskRequest configureTaskRequest) {
        Map<String, Object> variables = configureTaskRequest.getProcessVariables();

        String caseId = (String) variables.get(CASE_ID.value());
        String taskName = (String) variables.get(TASK_NAME.value());
        String taskTypeId = (String) variables.get(CamundaVariableDefinition.TASK_ID.value());

        log.info("Get configuration for case (id={}, name={}, taskType={})", caseId, taskName, taskTypeId);

        ConfigureTaskResponse response =
            configureTaskService.getConfiguration(
                new TaskToConfigure(
                    taskId,
                    taskTypeId,
                    caseId,
                    taskName,
                    null
                )
            );

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noCache())
            .body(response);
    }


}
