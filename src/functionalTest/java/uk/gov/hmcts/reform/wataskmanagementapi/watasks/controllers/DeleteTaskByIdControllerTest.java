package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.Map;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class DeleteTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private TestAuthenticationCredentials caseWorkerWithCftOrgRoles;

    @Before
    public void setUp() {
        caseWorkerWithCftOrgRoles = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.USER_WITH_CFT_ORG_ROLES);
    }

    @Test
    public void should_succeed_when_terminate_reason_is_cancelled() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);

        claimAndCancelTask(taskVariables);
        checkHistoryVariable(taskVariables.getTaskId(), "cftTaskState", "pendingTermination");


        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        checkHistoryVariable(taskVariables.getTaskId(), "cftTaskState", null);
    }

    @Test
    public void should_succeed_when_terminate_reason_is_completed() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);
        TestVariables testVariables = claimAndCompleteTask(taskVariables);
        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", "pendingTermination");

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            terminateTaskRequest,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", null);

    }

    private void checkHistoryVariable(String taskId, String variable, String value) {

        Map<String, Object> request = Map.of(
            "variableName", variable,
            "taskIdIn", singleton(taskId)
        );

        Response result = taskFunctionalTestsApiUtils.getCamundaApiActions().post(
            "/history/variable-instance",
            request,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        if (value == null) {
            //Should assert that it doesn't exist
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .contentType(APPLICATION_JSON_VALUE)
                .body("size()", equalTo(0));
        } else {
            //Should assert that value matches
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .contentType(APPLICATION_JSON_VALUE)
                .body("size()", equalTo(1))
                .body("name", hasItem(variable))
                .body("value", hasItem(value));
        }
    }

    private TestVariables claimAndCancelTask(TestVariables taskVariables) {
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "task/{task-id}/cancel",
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return taskVariables;
    }


    private TestVariables claimAndCompleteTask(TestVariables taskVariables) {
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "task/{task-id}/complete",
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");

        return taskVariables;
    }

}

