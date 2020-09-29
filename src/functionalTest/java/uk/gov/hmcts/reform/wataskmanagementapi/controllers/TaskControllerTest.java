package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;

import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringIntegrationSerenityRunner.class)
public class TaskControllerTest {

    private final String testUrl = System.getenv("TEST_URL") == null ? "http://localhost:8090" : System.getenv(
        "TEST_URL");

    @Test
    public void should_respond_with_200_with_dummy_task_id() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam("task-id", taskId)
            .baseUri(testUrl)
            .when()
            .get("task/{task-id}")
            .then()
            .statusCode(HttpStatus.OK_200);
    }


    @Test
    public void should_return_503_for_work_in_progress_endpoints() {
        String taskId = UUID.randomUUID().toString();
        String responseMessage = "Code is not implemented";

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .and().log().all(true)
            .body(new AssignTaskRequest("some-userid"))
            .when()
            .post("/task/{task-id}/assign")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503);


        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .when()
            .post("/task")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503)
            .body(equalTo(responseMessage));

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503)
            .body(equalTo(responseMessage));

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503)
            .body(equalTo(responseMessage));

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/unclaim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503)
            .body(equalTo("Code is not implemented"));

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/complete")
            .then()
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503)
            .body(equalTo(responseMessage));

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .and().log().all(true)
            .body(new AssignTaskRequest("some-user-id"))
            .when()
            .post("/task/{task-id}/assign")
            .then()
            .and().log().all(true)
            .assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE_503);

    }

}
