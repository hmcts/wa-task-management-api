package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.post(
            "task/{task-id}/assignee",
            nonExistentTaskId,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        //FIXME: This endpoint should be /assign
        //FIXME: This endpoint should return a 404
        //FIXME: error message

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()))
            .body(
                "message",
                equalTo(String.format(
                    "Cannot modify variables for task %s: task %s doesn't exist: task is null",
                    nonExistentTaskId,
                    nonExistentTaskId,
                    nonExistentTaskId
                ))
            );
    }

    @Test
    public void should_return_a_204_when_assigning_a_task_by_id() {


        Map<String, String> task = common.setupTaskAndRetrieveIds();

        Response result = restApiActions.post(
            "task/{task-id}/assignee",
            task.get("taskId"),
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "assigned");
    }

}

