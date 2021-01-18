package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";
    private String taskId;


    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        Headers headers = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        Map<String, String> task = setupScenario(headers);
        var taskId = task.get("taskId");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            headers

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskId, "taskState", "unassigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }


    private Map<String, String> setupScenario(Headers headers) {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            headers
        );

        return task;
    }

}

