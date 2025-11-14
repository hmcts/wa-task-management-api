package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskCancelByIdControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";

    TestAuthenticationCredentials caseWorkerWithJudgeRole;
    TestAuthenticationCredentials hearingPanelJudgeForStandardAccess;

    @Before
    public void setUp() {
        caseWorkerWithJudgeRole = taskFunctionalTestsUserUtils
            .getTestUser(CASE_WORKER_WITH_JUDGE_ROLE);
        hearingPanelJudgeForStandardAccess = taskFunctionalTestsUserUtils
            .getTestUser(CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS);
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, caseWorkerWithJudgeRole.getHeaders());

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

    @Test
    public void user_should_cancel_task_and_set_termination_process_for_valid_cancellation_process_and_flag_enabled() {

        TestAuthenticationCredentials caseworkerForReadCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseworkerForReadCredentials.getHeaders(), "judge");

        String[][] testData = {
            {"EXUI_USER_CANCELLATION", "EXUI_USER_CANCELLATION"},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        cancelTaskAndAssertTerminationProcess(testData, caseworkerForReadCredentials,
                                              "wa-user-with-cancellation-process-enabled-");
    }

    @Test
    public void user_should_cancel_task_and_not_set_termination_process_when_flag_disabled() {
        TestAuthenticationCredentials caseworkerForReadCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseworkerForReadCredentials.getHeaders(), "judge");

        String[][] testData = {
            {"EXUI_USER_CANCELLATION", null},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        cancelTaskAndAssertTerminationProcess(testData, caseworkerForReadCredentials,
                                              "wa-user-with-cancellation-process-disabled-");
    }

    private void cancelTaskAndAssertTerminationProcess(String[][] testData,
                                                       TestAuthenticationCredentials caseworkerForReadCredentials,
                                                       String userEmailPrefix) {

        for (String[] data : testData) {
            TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "reviewSpecificAccessRequestJudiciary", "Review Specific Access Request Judiciary");

            taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

            String taskId = taskVariables.getTaskId();
            TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
                authorizationProvider.getNewTribunalCaseworker(userEmailPrefix);
            taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
                caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
            String cancellationProcess = data[0];
            String terminationProcess = data[1];

            Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED + "?cancellation_process=" + cancellationProcess,
                taskId,
                caseWorkerWithLeadJudgeSpAccess.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", terminationProcess,
                caseworkerForReadCredentials.getHeaders()
            );

            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        }
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, hearingPanelJudgeForStandardAccess.getHeaders());

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

