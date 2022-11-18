package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TestAssertionsBuilder.buildTaskActionAttributesForAssertion;

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
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, Jurisdiction.WA);

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
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, Jurisdiction.WA);

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()), TaskAction.ASSIGN);

        common.cleanUpTask(taskId);
    }

    @Test
    //TODO: Remove, there is nothing specifically done in assigning an already assigned tasks
    public void assigner_has_grant_type_specific_and_permission_manage_can_assign_other_already_assigned_task() {
        //assigner role : manage
        //assignee role : execute
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, Jurisdiction.WA);

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()), TaskAction.ASSIGN);

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()), TaskAction.UNASSIGN_ASSIGN);

        common.cleanUpTask(taskId);
    }

    @Test
    //TODO: Remove, there is nothing specifically done in assigning an already assigned tasks
    public void assigner_has_grant_type_specific_and_permission_manage_own_can_assign_other_already_assigned_task() {
        //assigner role : manage, own
        //assignee role : manage, execute
        //second assignee role : read, manage, cancel
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "reviewSpecificAccessRequestJudiciary");
        taskId = taskVariables.getTaskId();

        common.setupCaseManagerForSpecificAccess(assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());

        //first assign
        common.setupFtpaJudgeForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()), TaskAction.ASSIGN);

        //second assign
        common.setupFtpaJudgeForSpecificAccess(secondAssigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(secondAssigneeCredentials.getHeaders()), TaskAction.UNASSIGN_ASSIGN);

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_assign_a_task_when_granular_permission_satisfied() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupChallengedAccessAdmin(granularPermissionCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupChallengedAccessLegalOps(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables, Jurisdiction.WA);

        String assigneeId = getAssigneeId(assigneeCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignmentForChallengedAccess(granularPermissionCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned", granularPermissionCaseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee", assigneeId, granularPermissionCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_not_assign_a_task_when_granular_permission_not_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        initiateTask(taskVariables, Jurisdiction.WA);

        common.setupCaseManagerForSpecificAccess(granularPermissionCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

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

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId, TaskAction taskAction) {

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

        String assignerId = getAssigneeId(assignerCredentials.getHeaders());
        Map<String, Matcher<?>> valueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "assigned", assignerId, taskAction);
        assertions.taskAttributesVerifier(taskId, valueMap, assignerCredentials.getHeaders());
    }

}

