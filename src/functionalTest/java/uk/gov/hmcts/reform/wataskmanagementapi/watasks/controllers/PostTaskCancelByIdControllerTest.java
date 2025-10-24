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
public class PostTaskCancelByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";

    TestAuthenticationCredentials caseWorkerWithJudgeRole;
    TestAuthenticationCredentials hearingPanelJudgeForStandardAccess;

    @Before
    public void setUp() {
        caseWorkerWithJudgeRole = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_JUDGE_ROLE);
        hearingPanelJudgeForStandardAccess = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS);
    }

    @Test
    public void user_should_not_cancel_task_when_role_assignment_verification_failed() {

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

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
    public void user_should_cancel_task_when_role_assignment_verification_passed() {

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
                                      "Review Specific Access Request Judiciary");

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables, caseWorkerWithJudgeRole.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithLeadJudgeSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for cancel request and then remove this.
    @Test
    public void user_should_cancel_task_when_grant_type_challenged_and_permission_cancel() {

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "reviewSpecificAccessRequestJudiciary",
            "Review Specific Access Request Judiciary");

        taskFunctionalTestsApiUtils.getCommon().setupChallengedAccessLegalOps(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, hearingPanelJudgeForStandardAccess.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithLeadJudgeSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }
}

