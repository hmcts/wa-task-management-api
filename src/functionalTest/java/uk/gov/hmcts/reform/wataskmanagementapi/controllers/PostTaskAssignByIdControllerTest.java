package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";
    private String taskId;



    private String getAssigneeId(Header caseworkerBAuthorizationOnly) {
        return authorizationHeadersProvider.getUserInfo(caseworkerBAuthorizationOnly.getValue()).getUid();
    }

    @Test
    public void should_return_a_204_when_assigning_a_task_by_id() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerBAuthorizationOnly());
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskId, "taskState", "assigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }




}

