package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AwaitilityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static org.hamcrest.Matchers.equalTo;

@Import(AwaitilityTestConfig.class)
@SuppressWarnings("checkstyle:LineLength")
public class PostTaskClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        currentCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(currentCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(currentCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_claim_task_when_role_assignment_verification_failed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");

        common.setupLeadJudgeForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");

        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");

        common.setupCaseManagerForSpecificAccess(currentCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            currentCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCaseManagerForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        common.cleanUpTask(taskId);
    }

}

