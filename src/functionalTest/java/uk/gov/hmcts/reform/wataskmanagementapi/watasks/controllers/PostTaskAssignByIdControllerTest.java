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

import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";

    private TestAuthenticationCredentials assignerCredentials;
    private TestAuthenticationCredentials assigneeCredentials;
    private TestAuthenticationCredentials secondAssigneeCredentials;
    private GrantType testGrantType = GrantType.SPECIFIC;
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        assigneeCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        secondAssigneeCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        if (testGrantType == GrantType.CHALLENGED) {
            common.clearAllRoleAssignmentsForChallenged(assignerCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(assigneeCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(secondAssigneeCredentials.getHeaders());
        } else {
            common.clearAllRoleAssignments(assignerCredentials.getHeaders());
            common.clearAllRoleAssignments(assigneeCredentials.getHeaders());
            common.clearAllRoleAssignments(secondAssigneeCredentials.getHeaders());
        }
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(assigneeCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(secondAssigneeCredentials.getAccount().getUsername());
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_should_not_assign_a_task_to_assignee_with_read_permission() {
        //assigner role : manage
        //assignee role : read
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

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
    public void assigner_has_grant_type_specific_and_permission_manage_should_assign_a_task_to_assignee_with_own_permission() {
        //assigner role : manage
        //assignee role : own
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_should_assign_a_task_to_assignee_with_execute() {
        //assigner role : manage
        //assignee role : execute
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_can_assign_other_already_assigned_task() {
        //assigner role : manage
        //assignee role : execute
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_own_should_assign_a_task_to_assignee_with_manage_own_permission() {
        //assigner role : manage, own
        //assignee role : manage, own
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        assignTaskAndValidate(taskVariables, getAssigneeId(assignerCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_own_should_assign_a_task_to_assignee_with_own_permission() {
        //assigner role : manage, own
        //assignee role : manage, execute
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_own_can_assign_other_already_assigned_task() {
        //assigner role : manage, own
        //assignee role : manage, execute
        //second assignee role : read, manage, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_execute_manage_should_assign_a_task_to_assignee_with_execute_manage_permission() {
        //assigner role : execute, manage
        //assignee role : execute, manage
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupFtpaJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupFtpaJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assignerCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_execute_manage_should_assign_a_task_to_assignee_with_own_manage_permission() {
        //assigner role : execute, manage
        //assignee role : own, manage
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupFtpaJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_execute_manage_can_assign_other_already_assigned_task() {
        //assigner role : execute, manage
        //assignee role : own, manage
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupFtpaJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        //first assign
        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupCaseManagerForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permissions_read_manage_cancel_should_assign_a_task() {
        //assigner role : read, manage, cancel
        //assignee role : own, manage
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_read_manage_cancel_can_assign_other_already_assigned_task() {
        //assigner role : read, manage, cancel
        //assignee role : own, manage
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        //first assign
        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupCaseManagerForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_read_manage_own_cancel_should_assign_a_task() {
        //assigner role : read, manage, own, cancel
        //assignee role : read, manage, own, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        common.setupLeadJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_read_manage_own_cancel_can_assign_other_already_assigned_task() {
        //assigner role : read, manage, own, cancel
        //assignee role : read, manage, own, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupLeadJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        //first assign
        common.setupLeadJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupLeadJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_read_manage_execute_cancel_can_assign_task() {
        //assigner role : read, manage, execute, cancel
        //assignee role : read, manage, execute, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));


        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_read_manage_execute_cancel_can_assign_other_already_assigned_task() {
        //assigner role : read, manage, execute, cancel
        //assignee role : read, manage, execute, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        //first assign
        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        //second assign
        common.setupCaseManagerForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));
        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_challenged_and_permission_manage_should_not_assign_a_task_to_assignee_with_read_permission() {
        //assigner role : manage
        //assignee role : read
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessLegalOps(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        common.setupChallengedAccessJudiciary(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

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
            .body("detail", equalTo("Role Assignment Verification: The user being assigned the Task has failed the Role Assignment checks performed."));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_challenged_and_permission_own_manage_should_not_assign_a_task_to_assignee_with_cancel_permission() {
        //assigner role : own, manage
        //assignee role : cancel
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessAdmin(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupChallengedAccessLegalOps(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

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
            .body("detail", equalTo("Role Assignment Verification: The user being assigned the Task has failed the Role Assignment checks performed."));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_challenged_and_permission_manage_can_assign_task() {
        //assigner role : manage
        //assignee role : own, manage
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessJudiciary(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary",
            "review specific access request judiciary",
            "review specific access request judiciary");

        common.setupChallengedAccessAdmin(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_challenged_and_permission_read_manage_cancel_can_assign_task_user_has_permission_execute_manage() {
        //assigner role : read, manage, cancel
        //assignee role : execute, manage
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessLegalOps(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        common.setupChallengedAccessJudiciary(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_not_assign_task_when_assignee_grant_type_challenged_and_excluded() {

        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessLegalOps(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestLegalOps",
            "review specific access request legal ops",
            "review specific access request legal ops");

        common.setupChallengedAccessJudiciary(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupExcludedAccessJudiciary(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

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
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL));

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

        switch (testGrantType) {
            case SPECIFIC:
                common.setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
                break;
            case CHALLENGED:
                common.setupCFTOrganisationalRoleAssignmentForChallengedAccess(assignerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
                break;
            default:
        }

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned", assignerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee", assigneeId, assignerCredentials.getHeaders());
    }

}

