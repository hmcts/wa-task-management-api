package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCancelByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_cancel_task_when_role_assignment_verification_failed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        String taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

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
    public void user_should_cancel_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
                                                                       "Review Specific Access Request Judiciary");

        common.setupLeadJudgeForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        common.setupWAOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), "judge");

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_and_set_termination_process_for_valid_cancellation_process_and_flag_enabled() {

        common.setupWAOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), "judge");

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

        common.setupWAOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), "judge");

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
            TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
                                                                           "Review Specific Access Request Judiciary");

            initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

            String cancellationProcess = data[0];
            String terminationProcess = data[1];

            String taskId = taskVariables.getTaskId();
            waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(userEmailPrefix);
            common.setupLeadJudgeForSpecificAccess(waCaseworkerCredentials.getHeaders(),
                                                   taskVariables.getCaseId(), WA_JURISDICTION);

            Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED + "?cancellation_process=" + cancellationProcess,
                taskId,
                waCaseworkerCredentials.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", terminationProcess,
                caseworkerForReadCredentials.getHeaders()
            );

            common.cleanUpTask(taskId);
        }
    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for cancel request and then remove this.
    @Test
    public void user_should_cancel_task_when_grant_type_challenged_and_permission_cancel() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
                                                                       "Review Specific Access Request Judiciary");

        common.setupChallengedAccessLegalOps(waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupHearingPanelJudgeForStandardAccess(caseworkerForReadCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }
}

