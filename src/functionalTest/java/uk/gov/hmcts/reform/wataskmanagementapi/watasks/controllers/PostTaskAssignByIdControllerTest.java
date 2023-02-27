package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";

    private TestAuthenticationCredentials assignerCredentials;
    private TestAuthenticationCredentials assigneeCredentials;
    private TestAuthenticationCredentials secondAssigneeCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        assigneeCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        secondAssigneeCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        granularPermissionCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(assignerCredentials.getHeaders());
        common.clearAllRoleAssignments(assigneeCredentials.getHeaders());
        common.clearAllRoleAssignments(secondAssigneeCredentials.getHeaders());
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());

        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(assigneeCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(secondAssigneeCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void assigner_should_not_assign_a_task_to_assignee_when_role_assignment_verification_failed() {
        //assigner role : manage
        //assignee role : read
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables);

        common.setupLeadJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(getAssigneeId(assigneeCredentials.getHeaders())),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo("Role Assignment Verification: "
                                    + "The user being assigned the Task has failed the Role Assignment checks performed."));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_should_assign_a_task_to_assignee_when_role_assignment_verification_pass() {
        //assigner role : manage
        //assignee role : own
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables);

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_can_assign_other_already_assigned_task() {
        //assigner role : manage
        //assignee role : execute
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables);

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_own_can_assign_other_already_assigned_task() {
        //assigner role : manage, own
        //assignee role : manage, execute
        //second assignee role : read, manage, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
                                                                       "Review Specific Access Request Judiciary");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupWAOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), "judge");

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_assign_a_task_when_granular_permission_satisfied() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessAdmin(granularPermissionCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupChallengedAccessLegalOps(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables);

        String assigneeId = getAssigneeId(assigneeCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignment(granularPermissionCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned", granularPermissionCaseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee", assigneeId, granularPermissionCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_not_assign_a_task_when_granular_permission_not_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        common.setupCaseManagerForSpecificAccess(granularPermissionCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);


        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(getAssigneeId(assigneeCredentials.getHeaders())),
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER));

        common.cleanUpTask(taskId);
    }


    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId) {

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned", assignerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee", assigneeId, assignerCredentials.getHeaders());
    }

}

