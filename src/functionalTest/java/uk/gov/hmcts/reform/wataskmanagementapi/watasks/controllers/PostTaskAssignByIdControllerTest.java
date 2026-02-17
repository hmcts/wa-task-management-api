package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskAssignByIdControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";
    private String taskId;

    @Test
    public void assigner_should_not_assign_a_task_to_assignee_when_role_assignment_verification_failed() {
        //assigner role : manage
        //assignee role : read
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestAuthenticationCredentials caseWorkerWithPanelJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            caseWorkerWithPanelJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(taskFunctionalTestsUserUtils.getAssigneeId(caseWorkerWithLeadJudgeSpAccess.getHeaders())),
            caseWorkerWithPanelJudgeSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo("Role Assignment Verification: "
                                    + "The user being assigned the Task has failed "
                                    + "the Role Assignment checks performed."));

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithPanelJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithPanelJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void assigner_should_assign_a_task_to_assignee_when_role_assignment_verification_pass() {
        //assigner role : manage
        //assignee role : own
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials caseMngrWithSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(
            taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(caseMngrWithSpAccess.getHeaders()),caseWorkerWithLeadJudgeSpAccess);

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_can_assign_other_already_assigned_task() {
        //assigner role : manage
        //assignee role : execute
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestAuthenticationCredentials caseWorkerWithFtpaJudge =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        //first assign
        taskFunctionalTestsApiUtils.getCommon().setupFtpaJudgeForSpecificAccess(
            caseWorkerWithFtpaJudge.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithFtpaJudge.getHeaders()),caseWorkerWithLeadJudgeSpAccess);

        TestAuthenticationCredentials caseWorkerWithFtpaJudge2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        //second assign
        taskFunctionalTestsApiUtils.getCommon().setupFtpaJudgeForSpecificAccess(
            caseWorkerWithFtpaJudge2.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(
            taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(caseWorkerWithFtpaJudge2.getHeaders()),caseWorkerWithLeadJudgeSpAccess);

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithFtpaJudge.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithFtpaJudge.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithFtpaJudge2.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithFtpaJudge2.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void assigner_has_grant_type_specific_and_permission_manage_own_can_assign_other_already_assigned_task() {
        //assigner role : Read,Own,Manage,Unassign,Assign,Complete
        //assignee role : Manage,Execute,Unassign,Assign,Complete
        //second assignee role : Manage,Execute,Unassign,Assign,Complete
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "reviewSpecificAccessRequestJudiciary",
            "Review Specific Access Request Judiciary");
        taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials caseMngrWithSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, caseMngrWithSpAccess.getHeaders());

        TestAuthenticationCredentials caseWorkerWithFtpaJudge =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        //first assign
        taskFunctionalTestsApiUtils.getCommon().setupFtpaJudgeForSpecificAccess(
            caseWorkerWithFtpaJudge.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithFtpaJudge.getHeaders()),caseMngrWithSpAccess);

        TestAuthenticationCredentials caseWorkerWithFtpaJudge2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        //second assign
        taskFunctionalTestsApiUtils.getCommon().setupFtpaJudgeForSpecificAccess(
            caseWorkerWithFtpaJudge2.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithFtpaJudge2.getHeaders()),caseMngrWithSpAccess);

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithFtpaJudge.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithFtpaJudge.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithFtpaJudge2.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithFtpaJudge2.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId, TestAuthenticationCredentials assignerCredentials) {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            assignerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", assignerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee", assigneeId, assignerCredentials.getHeaders());
    }

}

