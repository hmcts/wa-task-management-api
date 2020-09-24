package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.post(
            "task/{task-id}/claim",
            nonExistentTaskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        //FIXME: Better error message

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", greaterThanOrEqualTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(
                String.format("Cannot find task with id %s: task is null", nonExistentTaskId)));
    }

    @Test
    public void should_return_a_204_when_claiming_a_task_by_id() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.post(
            "task/{task-id}/claim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void endpoint_should_be_idempotent_should_return_a_204_when_claiming_a_task_by_id() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.post(
            "task/{task-id}/claim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultAfterClaimedBySameUser = restApiActions.post(
            "task/{task-id}/claim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        resultAfterClaimedBySameUser.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        Map<String, String> task = common.setupTaskAndRetrieveIds();

        given.iClaimATaskWithIdAndAuthorization(
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        Response result = restApiActions.post(
            "task/{task-id}/claim",
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.CONFLICT.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.CONFLICT.value()))
            .body("message", equalTo(String.format(
                "Task '%s' is already claimed by someone else.",
                task.get("taskId")
            )));
    }

}

