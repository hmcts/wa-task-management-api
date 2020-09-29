package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AuthorizationHeadersProvider;

import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.ServiceTokenGeneratorConfiguration.SERVICE_AUTHORIZATION;

public class TaskControllerTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

   @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    private String caseId;

    @Autowired
    private String serviceAuthorizationToken;

    @Before
    public void setUp() {
        caseId = UUID.randomUUID().toString();
        serviceAuthorizationToken =
            authorizationHeadersProvider
                .getAuthorizationHeaders()
                .getValue(SERVICE_AUTHORIZATION);
    }


    @Test
    public void transition_creates_atask_with_default_due_date() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        given()
            .relaxedHTTPSValidation()
            .header(SERVICE_AUTHORIZATION, serviceAuthorizationToken)
            .contentType(APPLICATION_JSON_VALUE)
            .pathParam("task-id", taskId)
            .baseUri(testUrl)
            .when()
            .get("task/{task-id}")
            .then()
            .statusCode(HttpStatus.OK_200);
    }
    @Test
    public void should_return_403_if_task_does_not_exist() {
        String taskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";
        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
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
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .when()
            .post("/task")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/claim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/unclaim")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/assign")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);

        given()
            .relaxedHTTPSValidation()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(testUrl)
            .pathParam("task-id", taskId)
            .when()
            .post("/task/{task-id}/complete")
            .then()
            .assertThat()
            .statusCode(HttpStatus.FORBIDDEN_403);
    }
}
