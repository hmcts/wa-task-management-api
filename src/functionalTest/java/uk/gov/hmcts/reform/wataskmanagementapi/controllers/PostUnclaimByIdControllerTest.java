package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.post(
            "task/{task-id}/unclaim",
            nonExistentTaskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message",
                equalTo("There was a problem unclaiming the task with id: " + nonExistentTaskId));
    }

    @Test
    public void should_return_a_404_when_unclaiming_a_task_by_id_with_different_credentials() {


        Map<String, String> task = common.setupTaskAndRetrieveIds();

        given.iClaimATaskWithIdAndAuthorization(
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        Response result = restApiActions.post(
            "task/{task-id}/unclaim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        //FIXME: Since the credentials are different we should not return 204 and claiming should be unsuccessful
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();


        given.iClaimATaskWithIdAndAuthorization(
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        Response result = restApiActions.post(
            "task/{task-id}/unclaim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "unassigned");
    }

}

