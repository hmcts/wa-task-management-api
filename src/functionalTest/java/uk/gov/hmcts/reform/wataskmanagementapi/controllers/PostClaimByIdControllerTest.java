package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";


    @Test
    public void should_return_a_204_when_claiming_a_task_by_id() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            task.get("taskId"),
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskId, "taskState", "assigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void endpoint_should_be_idempotent_should_return_a_204_when_claiming_a_task_by_id() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "assigned");

        Response resultAfterClaimedBySameUser = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        resultAfterClaimedBySameUser.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskId, "taskState", "assigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }


    @Test
    public void should_return_a_204_and_claim_a_task_by_id_jurisdiction_location_and_region_match() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        assertions.taskVariableWasUpdated(taskId, "taskState", "assigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }



}

