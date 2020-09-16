package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.Matchers.contains;

@RunWith(SpringIntegrationSerenityRunner.class)
public class TaskControllerTest {

    private final String testUrl = System.getenv("TEST_URL") == null ? "http://localhost:8099" :  System.getenv("TEST_URL");

    @Test
    public void should_return_404_if_task_does_not_exist() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .pathParam("task-id",taskId)
            .baseUri(testUrl)
            .when()
            .get("task/{task-id}")
            .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .and().body(contains("There was a problem fetching the task with id: " + taskId));
    }
}
