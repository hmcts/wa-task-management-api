package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdIdGenerator;

import java.util.List;
import java.util.UUID;

import static net.serenitybdd.rest.SerenityRest.expect;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class TaskControllerTest extends SpringBootFunctionalBaseTest {

    @Value("${targets.instance}")
    private String testUrl;

    private CcdIdGenerator ccdIdGenerator;

    @Before
    public void setUp() {

        ccdIdGenerator = new CcdIdGenerator();
        RestAssured.baseURI = testUrl;
        RestAssured.useRelaxedHTTPSValidation();
        given().contentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    public void should_return_404_if_task_does_not_exist() {
        String nonExistentTaskId = "78c9fc54-f1fb-11ea-a751-527f3fb68fa8";

        expect()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo("There was a problem fetching the task with id: " + nonExistentTaskId))
            .when()
            .get("task/{task-id}", nonExistentTaskId);

    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id() {

        String ccdId = ccdIdGenerator.generate();

        List<CamundaTask> response = given
            .iCreateATaskWithCcdId(ccdId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

        String taskId = response.get(0).getId();

        expect()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("id", equalTo(taskId))
            .when()
            .get("task/{task-id}", taskId);
    }

    @Test
    public void should_return_a_204_when_claiming_a_task_by_id() {

        String ccdId = ccdIdGenerator.generate();

        List<CamundaTask> tasks = given
            .iCreateATaskWithCcdId(ccdId)
            .and()
            .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

        String taskId = tasks.get(0).getId();

        Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .post("task/{task-id}/claim", taskId);

        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void should_return_503_for_work_in_progress_endpoints() {
        String taskId = UUID.randomUUID().toString();

        expect()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .when()
            .post("/task");

        expect()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .when()
            .post("/task/{task-id}/claim", taskId);


        expect()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .when()
            .post("/task/{task-id}/unclaim", taskId);

        expect()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .when()
            .post("/task/{task-id}/assign", taskId);

        expect()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.SERVICE_UNAVAILABLE.value()))
            .when()
            .post("/task/{task-id}/complete", taskId);

    }
}
