package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.util.Map;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class DeleteTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_succeed_when_terminate_reason_is_cancelled() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);

        claimAndCancelTask(taskVariables);
        checkHistoryVariable(taskVariables.getTaskId(), "cftTaskState", "pendingTermination");


        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        checkHistoryVariable(taskVariables.getTaskId(), "cftTaskState", null);
    }


    @Test
    public void should_succeed_when_terminate_reason_is_completed() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);
        TestVariables testVariables = claimAndCompleteTask(taskVariables);
        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", "pendingTermination");

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            terminateTaskRequest,
            caseworkerCredentials.getHeaders()
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

        Response result = camundaApiActions.post(
            "/history/variable-instance",
            request,
            caseworkerCredentials.getHeaders()
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

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            "task/{task-id}/cancel",
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return taskVariables;
    }


    private TestVariables claimAndCompleteTask(TestVariables taskVariables) {
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            "task/{task-id}/complete",
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");

        return taskVariables;
    }

}

