package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCancelByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/cancel";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;
    private TestAuthenticationCredentials assignerCredentials;
    private GrantType testGrantType = GrantType.SPECIFIC;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        granularPermissionCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-");
        assignerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        common.clearAllRoleAssignments(assignerCredentials.getHeaders());
        common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());

        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_cancel_task_when_role_assignment_verification_failed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        String taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables, Jurisdiction.WA);

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
    public void user_should_cancel_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "reviewSpecificAccessRequestJudiciary");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

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
    public void user_should_cancel_task_when_granular_permission_cancel_own_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, Jurisdiction.WA);

        common.setupCFTOrganisationalRoleAssignmentForWA(granularPermissionCaseworkerCredentials.getHeaders());
        String taskId = taskVariables.getTaskId();
        Response resultAssignee = restApiActions.post(
            "task/{task-id}/assign",
            taskId,
            new AssignTaskRequest(getAssigneeId(granularPermissionCaseworkerCredentials.getHeaders())),
            assignerCredentials.getHeaders()
        );
        // Now that we have a task with 'cancel_own' permission with assignee same as cancelling user
        resultAssignee.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            granularPermissionCaseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "pendingTermination"
        );

        common.cleanUpTask(taskId);
    }

}

