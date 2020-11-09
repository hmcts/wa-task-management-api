package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;

public class GetTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.get(
            "task/{task-id}",
            nonExistentTaskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo("There was a problem fetching the task with id: " + nonExistentTaskId));
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_any_roles() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo("User did not have sufficient permissions to access task"));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_match() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(task.get("taskId")));
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_jurisdiction_did_not_match() {
        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariable(JURISDICTION, "SSCS");

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(
                format("User did not have sufficient permissions to access task with id: %s", task.get("taskId"))
            ));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_and_region_match() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(task.get("taskId")));
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariable(REGION, "north-england");

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(
                format("User did not have sufficient permissions to access task with id: %s", task.get("taskId"))
            ));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_and_location_match() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerCAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(task.get("taskId")));
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_location_did_not_match() {

        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariable(LOCATION, "17595");

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerCAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(
                format("User did not have sufficient permissions to access task with id: %s", task.get("taskId"))
            ));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_region_and_location_match() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerDAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(task.get("taskId")));
    }

    @Test
    public void should_return_a_403_when_user_did_not_have_permission_jurisdiction_region_location_did_not_match() {
        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariable(LOCATION, "17595");

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerDAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", equalTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(
                format("User did not have sufficient permissions to access task with id: %s", task.get("taskId"))
            ));
    }

}

