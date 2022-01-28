package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Map;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class Assertions {

    private final RestApiActions camundaApiActions;
    private final RestApiActions restApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public Assertions(RestApiActions camundaApiActions, RestApiActions restApiActions,
                      AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaApiActions = camundaApiActions;
        this.restApiActions = restApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
    }

    public void checkHistoryVariableWasDeleted(String historyVariableId) {

        Response result = camundaApiActions.get(
            "/history/variable-instance/{id}",
            historyVariableId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("size()", equalTo(0));
    }

    /**
     * The following method will call the history table to look for a specific variable.
     * This call is expensive and can be time consuming on the database to optimize the query:
     * - Limit result by variable name
     * - Limit max results to 1 as there should only be one
     * - Return the id in case we then need to check if it has been updated
     *
     * @param taskId   the task id
     * @param variable the variable name to look for
     * @param value    the value used in the assertion
     */
    public String checkHistoryVariable(String processInstanceId, String taskId, String variable, String value) {

        Map<String, Object> request = Map.of(
            "processInstanceId", processInstanceId,
            "variableName", variable,
            "variableValue", value,
            "taskIdIn", singleton(taskId),
            "maxResults", 1
        );

        Response result = camundaApiActions.post(
            "/history/variable-instance",
            request,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        return result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("size()", equalTo(1))
            .extract()
            .path("[0].id");


    }

    public void taskVariableWasUpdated(String processInstanceId, String variable, String value) {

        Map<String, Object> request = Map.of(
            "variableName", variable,
            "variableValue", value,
            "processInstanceId", processInstanceId,
            "maxResults", 1
        );

        Response result = camundaApiActions.post(
            "/history/variable-instance",
            request,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("size()", equalTo(1));

    }

    public void taskStateWasUpdatedinDatabase(String taskId, String value, Headers authenticationHeaders) {

        Response result = restApiActions.get(
            "task/{task-id}",
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

    public void taskFieldWasUpdatedInDatabase(String taskId, String fieldName, String value,
                                              Headers authenticationHeaders) {

        Response result = restApiActions.get(
            "task/{task-id}",
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
