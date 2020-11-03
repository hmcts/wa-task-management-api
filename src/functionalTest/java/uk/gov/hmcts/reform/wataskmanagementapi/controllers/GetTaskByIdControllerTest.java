package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class GetTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.get(
            "task/{task-id}",
            nonExistentTaskId,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(instanceOf(String.class)))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo("There was a problem fetching the task with id: " + nonExistentTaskId));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        //Call role assignment to assign case to user
        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getLawFirmBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(task.get("taskId")));
    }


    @Test
    public void should_return_a_403_when_the_user_did_not_have_any_roles() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(instanceOf(String.class)))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo("User did not have sufficient permissions to access task"));
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission() {
        Headers headers = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        Map<String, String> task = common.setupTaskWithRoleAssignmentAndRetrieveIds(headers, "tribunal-caseworker");

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(instanceOf(String.class)))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo("User did not have sufficient permissions to access task"));
    }

}

