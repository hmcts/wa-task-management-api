package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Assertions {
    private static final String TASK_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String CAMUNDA_SEARCH_HISTORY_ENDPOINT = "/history/variable-instance";

    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationProvider authorizationProvider;
    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public Assertions(RestApiActions camundaApiActions, RestApiActions restApiActions,
                      AuthorizationProvider authorizationProvider,
                      CFTTaskDatabaseService cftTaskDatabaseService) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    public void taskVariableWasUpdated(String processInstanceId, String variable, String value) {

        Map<String, Object> request = Map.of(
            "variableName", variable,
            "processInstanceId", processInstanceId
        );

        Response result = camundaApiActions.post(
            CAMUNDA_SEARCH_HISTORY_ENDPOINT,
            request,
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("name", everyItem(is(variable)))
            .body("value", hasItem(value));
    }

    public void taskStateWasUpdatedInDatabase(String taskId, String value, Headers authenticationHeaders) {

        Response result = restApiActions.get(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", equalTo(value))
            .log();
    }

    public void taskActionAttributesVerifier(String taskId,
                                             String assigneeId,
                                             String taskState,
                                             String lastUpdatedUserId,
                                             TaskAction taskAction) {

        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        if (taskResource.isPresent()) {
            TaskResource task = taskResource.get();
            assertAll("taskResource",
                () -> assertEquals(taskId, task.getTaskId()),
                () -> assertEquals(taskState, task.getState().getValue()),
                () -> assertEquals(assigneeId, task.getAssignee()),
                () -> assertNotNull(task.getLastUpdatedTimestamp()),
                () -> assertEquals(lastUpdatedUserId, task.getLastUpdatedUser()),
                () -> assertEquals(taskAction.getValue(), task.getLastUpdatedAction())
            );
        }
    }

    public void taskFieldWasUpdatedInDatabase(String taskId, String fieldName, String value,
                                              Headers authenticationHeaders) {

        Response result = restApiActions.get(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task." + fieldName, equalTo(value))
            .log();
    }
}
