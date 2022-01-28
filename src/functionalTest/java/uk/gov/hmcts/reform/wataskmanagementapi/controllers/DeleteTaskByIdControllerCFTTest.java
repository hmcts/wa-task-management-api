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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class DeleteTaskByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
    }

    @Test
    public void should_succeed_when_terminate_reason_is_cancelled() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        initiateTask(taskVariables);

        claimAndCancelTask(taskVariables);

        String historyVariableId = assertions.checkHistoryVariable(
            taskVariables.getTaskId(),
            "cftTaskState",
            "pendingTermination"
        );

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.checkHistoryVariableWasDeleted(historyVariableId);
    }


    @Test
    public void should_succeed_when_terminate_reason_is_completed() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        initiateTask(taskVariables);
        TestVariables testVariables = claimAndCompleteTask(taskVariables);
        String historyVariableId = assertions.checkHistoryVariable(
            taskVariables.getTaskId(),
            "cftTaskState",
            "pendingTermination"
        );

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response result = restApiActions.delete(
            ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            terminateTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.checkHistoryVariableWasDeleted(historyVariableId);

    }

    private void initiateTask(TestVariables testVariables) {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()),
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
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

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);
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
        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);
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

