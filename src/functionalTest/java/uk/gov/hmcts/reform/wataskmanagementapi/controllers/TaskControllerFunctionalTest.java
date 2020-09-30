package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AuthorizationHeadersProvider;

import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class TaskControllerFunctionalTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_respond_with_200_with_dummy_task_id() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        Headers authHeaders =
            authorizationHeadersProvider
                .getAuthorizationHeaders();

        given()
            .relaxedHTTPSValidation()
            .headers(authHeaders)
            .contentType(APPLICATION_JSON_VALUE)
            .pathParam("task-id", taskId)
            .baseUri(testUrl)
            .when()
            .get("/task/{task-id}")
            .then()
            .statusCode(HttpStatus.OK_200);
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
            .post("/task/{task-id}/complete")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

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
            .statusCode(HttpStatus.FORBIDDEN_403);

    }

}
