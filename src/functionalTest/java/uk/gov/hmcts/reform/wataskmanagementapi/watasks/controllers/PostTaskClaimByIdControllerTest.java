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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials currentCaseworkerCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;
    private GrantType testGrantType = GrantType.SPECIFIC;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        currentCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        granularPermissionCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-");
    }

    @After
    public void cleanUp() {
        if (testGrantType == GrantType.CHALLENGED) {
            common.clearAllRoleAssignmentsForChallenged(caseworkerCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(currentCaseworkerCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(granularPermissionCaseworkerCredentials.getHeaders());
        } else {
            common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
            common.clearAllRoleAssignments(currentCaseworkerCredentials.getHeaders());
            common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());
        }
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());

        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(currentCaseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_claim_task_when_grant_type_specific_and_permission_read() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permission_own() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permission_execute() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupFtpaJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                               WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_not_claim_task_when_grant_type_specific_and_permission_manage() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupHearingPanelJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                       WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_not_claim_task_when_grant_type_specific_and_permission_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permissions_own_manage() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permissions_execute_manage() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupFtpaJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                               WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_not_claim_task_when_grant_type_specific_and_permissions_read_manage_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupHearingPanelJudgeForSpecificAccess(caseworkerCredentials.getHeaders(),
                                                       taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permissions_execute_read_manage_own_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestLegalOps");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_specific_and_permissions_execute_read_manage_execute_cancel() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestLegalOps");

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCaseManagerForSpecificAccess(currentCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            currentCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_not_claim_task_when_grant_type_challenged_and_permission_read() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupChallengedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                              WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_challenged_and_permissions_execute_assign() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupChallengedAccessAdmin(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                          WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_not_claim_task_when_grant_type_challenged_and_permission_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupChallengedAccessLegalOps(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                             WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        String taskId = taskVariables.getTaskId();
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
    public void user_should_not_claim_task_when_grant_type_challenged_and_permission_read_manage_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestLegalOps");

        common.setupChallengedAccessLegalOps(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                             WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_grant_type_challenged_and_permissions_read_manage_own_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestLegalOps");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessAdmin(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                          WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

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
    public void user_should_claim_task_when_grant_type_challenged_and_permissions_read_manage_execute_cancel() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestAdmin");
        String taskId = taskVariables.getTaskId();

        common.setupChallengedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                              WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerCredentials.getHeaders());

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
    public void user_should_not_claim_task_when_grant_type_challenged_and_excluded() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupChallengedAccessAdmin(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                          WA_JURISDICTION, WA_CASE_TYPE);
        common.setupExcludedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                            WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
                                                            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());
        String taskId = taskVariables.getTaskId();

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
    public void user_should_claim_task_when_grant_type_specific_and_excluded() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        common.setupExcludedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                            WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();
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
    public void user_should_claim_task_when_granular_permission_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignmentForWA(granularPermissionCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.WA);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_not_claim_a_task_when_granular_permission_not_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");
        String taskId = taskVariables.getTaskId();

        common.setupStandardCaseManager(granularPermissionCaseworkerCredentials.getHeaders(),
                                                 taskVariables.getCaseId(), "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            granularPermissionCaseworkerCredentials.getHeaders()
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
}

