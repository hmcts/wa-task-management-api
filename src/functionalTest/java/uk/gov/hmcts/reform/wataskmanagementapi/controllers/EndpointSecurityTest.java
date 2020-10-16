package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;

import static net.serenitybdd.rest.SerenityRest.given;

public class EndpointSecurityTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

    @Before
    public void setUp() {

        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void should_return_403_when_no_service_token_is_provided() {
        String taskId = "00000000-0000-0000-0000-000000000000";

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .when()
            .post("/task")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/unclaim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/complete")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .relaxedHTTPSValidation()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .body(new AssignTaskRequest("some-user-id"))
            .when()
            .post("/task/{task-id}/assign")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

    }

}
