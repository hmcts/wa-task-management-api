package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.CURRENT_CASE_WORKER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.WA_CASE_WORKER;


@SuppressWarnings("checkstyle:LineLength")
public class PostTaskClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    TestAuthenticationCredentials waCaseworkerCredentials;
    TestAuthenticationCredentials currentCaseworkerCredentials;

    @Before
    public void setUp() {
        waCaseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(WA_CASE_WORKER);
        currentCaseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(CURRENT_CASE_WORKER);
    }

    @After
    public void cleanUp() {
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(currentCaseworkerCredentials.getHeaders());
    }

    @Test
    public void user_should_not_claim_task_when_role_assignment_verification_failed() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            currentCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            currentCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

}

