package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;

import static net.serenitybdd.rest.SerenityRest.given;

@RunWith(SpringIntegrationSerenityRunner.class)
public class TaskControllerTest {

    private final String testUrl = System.getenv("TEST_URL") == null ? "http://localhost:8099" :  System.getenv("TEST_URL");

    @Test
    public void transitionGetsATask() {
        given()
            .relaxedHTTPSValidation()
            .contentType("application/json")
            .pathParam("task-id","78c9fc54-f1fb-11ea-a751-527f3fb68fa8")
            .baseUri(testUrl)
            .when()
            .get("task/{task-id}")
            .then()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }
}
