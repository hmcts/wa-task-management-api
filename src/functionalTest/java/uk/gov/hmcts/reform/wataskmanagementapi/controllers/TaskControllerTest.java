package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdIdGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AuthorizationHeadersProvider;

import java.util.List;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class TaskControllerTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

    private CcdIdGenerator ccdIdGenerator;
    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Before
    public void setUp() {

        ccdIdGenerator = new CcdIdGenerator();
        ccdIdGenerator = new CcdIdGenerator();
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void should_return_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .get("task/{task-id}", nonExistentTaskId);

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo("There was a problem fetching the task with id: " + nonExistentTaskId));


    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id() {

        String ccdId = ccdIdGenerator.generate();

        List<CamundaTask> response = given
            .iCreateATaskWithCcdId(ccdId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

        String taskId = response.get(0).getId();

        Response result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .get("task/{task-id}", taskId);

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId));
    }

    @Test
    public void should_return_a_204_when_claiming_a_task_by_id() {

        String ccdId = ccdIdGenerator.generate();

        List<CamundaTask> tasks = given
            .iCreateATaskWithCcdId(ccdId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

        String taskId = tasks.get(0).getId();

        Response result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .post("task/{task-id}/claim", taskId);

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void should_return_503_for_work_in_progress_endpoints() {
        String taskId = UUID.randomUUID().toString();
        String responseMessage = "Code is not implemented";

        Response result;

        result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .post("/task");

        result.then().assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .body("message", equalTo(responseMessage));

        result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .post("/task/{task-id}/unclaim", taskId);

        result.then().assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .body("message", equalTo(responseMessage));

        result = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(authorizationHeadersProvider.getAuthorizationHeaders())
            .when()
            .post("/task/{task-id}/complete", taskId);

        result.then().assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .body("message", equalTo(responseMessage));
    }
}
