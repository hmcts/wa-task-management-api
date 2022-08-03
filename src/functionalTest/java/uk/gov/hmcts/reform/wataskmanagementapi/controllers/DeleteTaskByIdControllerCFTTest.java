package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;

public class DeleteTaskByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        initiateTask(taskVariables);

        claimAndCancelTask(taskVariables);
        checkHistoryVariable(taskVariables.getTaskId(), "cftTaskState", "pendingTermination");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        initiateTask(taskVariables);
        TestVariables testVariables = claimAndCompleteTask(taskVariables);
        checkHistoryVariable(testVariables.getTaskId(), "cftTaskState", "pendingTermination");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

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

    private void initiateTask(TestVariables testVariables) {
        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
            CASE_ID.value(), testVariables.getCaseId(),
            CREATED.value(), formattedCreatedDate,
            DUE_DATE.value(), formattedDueDate
        );

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, taskAttributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            req,
            caseworkerCredentials.getHeaders()
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

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
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
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
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

