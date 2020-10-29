package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostTaskCompleteByIdControllerTest extends SpringBootFunctionalBaseTest {


    @Value("${targets.instance}")
    private String testUrl;

    @Value("${targets.camunda}")
    private String camundaUrl;

    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.post(
            "task/{task-id}/complete",
            nonExistentTaskId,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );


        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(
                String.format("There was a problem completing the task with id: %s", nonExistentTaskId)));
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.post(
            "task/{task-id}/complete",
            task.get("taskId"),
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "completed");
    }

    @Test
    public void endpoint_should_be_idempotent_should_return_a_204_when_completing_an_already_completed_task() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();
        Headers headers = authorizationHeadersProvider.getLawFirmAAuthorization();

        Response result = restApiActions.post(
            "task/{task-id}/complete",
            task.get("taskId"),
            headers
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "completed");

        Response resultWhenTaskAlreadyCompleted = restApiActions.post(
            "task/{task-id}/complete",
            task.get("taskId"),
            headers
        );

        resultWhenTaskAlreadyCompleted.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

}

