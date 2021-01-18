package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_DELETED;

public class PostTaskCancelByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";


    @Test
    public void should_return_a_204_when_cancelling_a_task_by_id() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId, REASON_DELETED);
    }

}
