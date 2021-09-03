package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_TYPE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class DeleteTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        common.clearAllRoleAssignments(authenticationHeaders);
    }

    @Test
    public void should_succeed_when_terminate_reason_is_cancelled() {

        TestVariables testVariables = common.setupTaskAndRetrieveIds();
        initiateTask(testVariables);
        claimAndCancelTask(testVariables);
        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", "pendingTermination");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo(TerminateReason.CANCELLED)
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            terminateTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", null);
    }


    @Test
    public void should_succeed_when_terminate_reason_is_completed() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        initiateTask(taskVariables);
        TestVariables testVariables = claimAndCompleteTask(taskVariables);
        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", "pendingTermination");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo(TerminateReason.COMPLETED)
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            terminateTaskRequest,
            authenticationHeaders
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
            authenticationHeaders
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

    private void initiateTask(TestVariables testVariables) {

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()),
            new TaskAttribute(TASK_TYPE, "reviewTheAppeal"),
            new TaskAttribute(TASK_NAME, "Review The Appeal")
        ));

        Response result = restApiActions.post(
            "task/{task-id}",
            testVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }


    private TestVariables claimAndCancelTask(TestVariables taskVariables) {
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );
        Response result = restApiActions.post(
            "task/{task-id}/cancel",
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        return taskVariables;
    }


    private TestVariables claimAndCompleteTask(TestVariables taskVariables) {
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );
        Response result = restApiActions.post(
            "task/{task-id}/complete",
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");

        return taskVariables;
    }

}

