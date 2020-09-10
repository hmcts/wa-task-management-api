package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;

import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;

@RunWith(SpringIntegrationSerenityRunner.class)
public class TaskControllerTest {

    private final String testUrl = System.getenv("TEST_URL") == null ? "http://localhost:8090" : System.getenv(
        "TEST_URL");

    @Test
    public void should_return_403_if_task_does_not_exist() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("task-id", taskId)
            .baseUri(testUrl)
            .when()
            .get("task/{task-id}")
            .then()
            .statusCode(HttpStatus.FORBIDDEN_403);
    }


    @Test
    public void should_return_403_for_work_in_progress_endpoints() {
        String taskId = UUID.randomUUID().toString();
        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .when()
            .post("/task")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/unclaim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/assign")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/complete")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);
    }
}
