package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;

public class Assertions {
    private static final String TASK_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String CAMUNDA_SEARCH_HISTORY_ENDPOINT = "/history/variable-instance";

    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationProvider authorizationProvider;

    public Assertions(RestApiActions camundaApiActions, RestApiActions restApiActions,
                      AuthorizationProvider authorizationProvider) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationProvider = authorizationProvider;
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

    public void taskStateWasUpdatedInDatabase(String taskId, List<String> states, Headers authenticationHeaders) {

        Response result = restApiActions.get(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", in(states))
            .log();
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
