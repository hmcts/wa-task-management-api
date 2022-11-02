package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class Assertions {

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
            "/history/variable-instance",
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

    public void taskValuesWasUpdatedInDatabase(String taskId, Map<String, Matcher<?>> fieldValueMap,
                                               Headers authenticationHeaders) {

        Response result = restApiActions.get(
            "task/{task-id}",
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE).log();

        fieldValueMap.entrySet().forEach(
            entry -> result.then().assertThat()
                .body(entry.getKey(), entry.getValue()).log()
        );
    }

    public void taskActionAttributesUpdatedInDatabase(String taskId, String assigneeId,
                                                      String taskState, String lastUpdatedUserId, TaskAction taskAction,
                                                      TestAuthenticationCredentials taskRetrieveCredentials) {

        Map<String, Matcher<?>> taskValueMap = Map.of(
            "task.id", equalTo(taskId),
            "task.task_state", CoreMatchers.is(taskState),
            "task.assignee", equalTo(assigneeId),
            "task.last_updated_timestamp", notNullValue(),
            "task.last_updated_user", equalTo(lastUpdatedUserId),
            "task.last_updated_action", equalTo(taskAction.getValue())
        );

        taskValuesWasUpdatedInDatabase(taskId, taskValueMap, taskRetrieveCredentials.getHeaders());
    }

    public void nullTaskActionAttributesUpdatedInDatabase(String taskId, String taskState,
                                                          TestAuthenticationCredentials taskRetrieveCredentials) {

        Map<String, Matcher<?>> taskValueMap = Map.of(
            "task.id", equalTo(taskId),
            "task.task_state", CoreMatchers.is(taskState),
            "task.assignee", nullValue(),
            "task.last_updated_timestamp", nullValue(),
            "task.last_updated_user", nullValue(),
            "task.last_updated_action", nullValue()
        );

        taskValuesWasUpdatedInDatabase(taskId, taskValueMap, taskRetrieveCredentials.getHeaders());
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
