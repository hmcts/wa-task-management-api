package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCancelByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";

    private TestAuthenticationCredentials caseworkerCredentials;
    private GrantType testGrantType = GrantType.SPECIFIC;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        if (testGrantType == GrantType.CHALLENGED) {
            common.clearAllRoleAssignmentsForChallenged(caseworkerCredentials.getHeaders());
        } else {
            common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        }
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_cancel_task_when_grant_type_specific_and_permission_read() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
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
    public void user_should_cancel_task_when_grant_type_specific_and_permission_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_specific_and_permissions_read_manage_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_specific_and_permissions_read_manage_own_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_specific_and_permissions_read_manage_execute_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_not_cancel_task_when_grant_type_challenged_and_permission_read() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
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
    public void user_should_cancel_task_when_grant_type_challenged_and_permission_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessLegalOps(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_challenged_and_permissions_read_manage_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessLegalOps(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_challenged_and_permissions_read_manage_own_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessAdmin(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_cancel_task_when_grant_type_challenged_and_permissions_read_manage_execute_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestAdmin",
            "review specific access request admin",
            "review specific access request admin");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

}

