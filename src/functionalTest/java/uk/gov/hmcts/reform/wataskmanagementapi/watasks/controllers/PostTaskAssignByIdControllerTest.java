package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AwaitilityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
@Import(AwaitilityTestConfig.class)
public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        assigneeCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        secondAssigneeCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(assigneeCredentials.getHeaders());
        authorizationProvider.deleteAccount(assigneeCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(secondAssigneeCredentials.getHeaders());
        authorizationProvider.deleteAccount(secondAssigneeCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
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
        //assigner role : Read,Own,Manage,Unassign,Assign,Complete
        //assignee role : Manage,Execute,Unassign,Assign,Complete
        //second assignee role : Manage,Execute,Unassign,Assign,Complete
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

