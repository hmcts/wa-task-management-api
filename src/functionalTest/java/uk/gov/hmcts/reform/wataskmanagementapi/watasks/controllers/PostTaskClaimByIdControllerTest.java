package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
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


@SuppressWarnings("checkstyle:LineLength")
public class PostTaskClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    TestAuthenticationCredentials caseWorkerWithTribRole;

    @Before
    public void setUp() {
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.USER_WITH_TRIB_CASEWORKER_ROLE);
    }

    @Test
    public void user_should_not_claim_task_when_role_assignment_verification_failed() {

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithLeadJudgeSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestAuthenticationCredentials caseMngrWithSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseMngrWithSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TestAuthenticationCredentials caseMngrWithSpAccess2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess2.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseMngrWithSpAccess2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess2.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess2.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

}

