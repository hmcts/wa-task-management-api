package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCompleteByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/complete";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";
    private static final String ASSIGN_ENDPOINT = "task/{task-id}/assign";

    private String assigneeId;
    private String taskId;

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        otherUser = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R2);

        assigneeId = getAssigneeId(waCaseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(otherUser.getHeaders());
        authorizationProvider.deleteAccount(otherUser.getAccount().getUsername());

        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_termination_process() {
        TestAuthenticationCredentials userWithCompletionProcessEnabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-enabled-");
        common.setupWAOrganisationalRoleAssignment(userWithCompletionProcessEnabled.getHeaders());

        String[][] testData = {
            {"EXUI_USER_COMPLETION", "EXUI_USER_COMPLETION"},
            {"EXUI_CASE-EVENT_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        for (String[] data : testData) {

            TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
            taskId = taskVariables.getTaskId();
            initiateTask(taskVariables);
            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, userWithCompletionProcessEnabled.getHeaders()
            );
            Response result = restApiActions.post(
                CLAIM_ENDPOINT,
                taskId,
                userWithCompletionProcessEnabled.getHeaders()
            );
            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, userWithCompletionProcessEnabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = restApiActions.post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                userWithCompletionProcessEnabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", terminationProcess, userWithCompletionProcessEnabled.getHeaders()
            );
            assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
            assertions.taskStateWasUpdatedInDatabase(taskId, "completed",
                                                     userWithCompletionProcessEnabled.getHeaders());

            common.cleanUpTask(taskId);
        }
        common.clearAllRoleAssignments(userWithCompletionProcessEnabled.getHeaders());
        authorizationProvider.deleteAccount(userWithCompletionProcessEnabled.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_null_termination_process_when_flag_disabled() {
        TestAuthenticationCredentials userWithCompletionProcessDisabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-disabled-");
        common.setupWAOrganisationalRoleAssignment(userWithCompletionProcessDisabled.getHeaders());

        String[][] testData = {
            {"EXUI_USER_COMPLETION", "EXUI_USER_COMPLETION"},
            {"EXUI_CASE-EVENT_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        for (String[] data : testData) {

            TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
            taskId = taskVariables.getTaskId();

            initiateTask(taskVariables);
            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, userWithCompletionProcessDisabled.getHeaders()
            );
            Response result = restApiActions.post(
                CLAIM_ENDPOINT,
                taskId,
                userWithCompletionProcessDisabled.getHeaders()
            );
            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, userWithCompletionProcessDisabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = restApiActions.post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                userWithCompletionProcessDisabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            assertions.taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, userWithCompletionProcessDisabled.getHeaders()
            );
            assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
            assertions.taskStateWasUpdatedInDatabase(taskId, "completed",
                                                     userWithCompletionProcessDisabled.getHeaders());

            common.cleanUpTask(taskId);
        }
        common.clearAllRoleAssignments(userWithCompletionProcessDisabled.getHeaders());
        authorizationProvider.deleteAccount(userWithCompletionProcessDisabled.getAccount().getUsername());
    }

    @Test
    public void should_return_a_403_if_task_was_not_previously_assigned() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_NOT_ASSIGNED,
                taskId
            )));

    }

    @Test
    public void should_succeed_and_return_204_when_a_task_that_was_already_claimed_and_privileged_auto_complete() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api
        common.setupWAOrganisationalRoleAssignment(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(new CompletionOptions(false));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            otherUser.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(waCaseworkerCredentials.getHeaders().getValue(AUTHORIZATION));

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_ASSIGNED_TO_OTHER_USER,
                taskId, userInfo.getUid()
            )));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_not_complete_when_a_task_was_already_claimed_and_privileged_auto_complete_is_false() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api
        common.setupWAOrganisationalRoleAssignment(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(new CompletionOptions(false));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            otherUser.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(waCaseworkerCredentials.getHeaders().getValue(AUTHORIZATION));

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_ASSIGNED_TO_OTHER_USER,
                taskId, userInfo.getUid()
            )));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_after_assign_complete_terminate_true() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", waCaseworkerCredentials.getHeaders());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response deleteResult = restApiActions.delete(
            "/task/{task-id}",
            taskId,
            terminateTaskRequest,
            waCaseworkerCredentials.getHeaders()
        );

        deleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskStateWasUpdatedInDatabase(taskId, "terminated", waCaseworkerCredentials.getHeaders());

        Response reCompleteResult = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(false)),
            waCaseworkerCredentials.getHeaders()
        );

        reCompleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskStateWasUpdatedInDatabase(taskId, "terminated", waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void user_should_complete_a_assigned_task() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        assignTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for complete request, then remove this.
    @Test
    public void should_return_a_204_when_completing_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        common.setupSpecificTribunalCaseWorker(taskVariables.getCaseId(), waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    //Need new IT to cover role assignment verification for attributes in common for all actions, then remove this test.
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = common.setupWATaskWithWithCustomVariableAndRetrieveIds(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());
        //Create temporary role-assignment to assign task
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.FORBIDDEN
        );

        //Delete role-assignment and re-create
        common.setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );


        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_user_does_not_have_permission() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");

        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "case-manager");
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "case-manager");

        String taskId = taskVariables.getTaskId();

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(waCaseworkerCredentials.getHeaders().getValue(AUTHORIZATION));

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_COULD_NOT_COMPLETE_TASK_WITH_ID_ASSIGNED_TO_OTHER_USER,
                taskId, userInfo.getUid()
            )));

        common.cleanUpTask(taskId);
    }

    private void assignTask(TestVariables taskVariables) {

        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        Response result = restApiActions.post(
            ASSIGN_ENDPOINT,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", waCaseworkerCredentials.getHeaders()
        );
        assertions.taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee", assigneeId, waCaseworkerCredentials.getHeaders()
        );

    }

    @Test
    public void should_return_a_case_role_assignment() {
        TestVariables taskVariables1 = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables1);
        TestVariables taskVariables2 = common.setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables2);

        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        String taskId1 = taskVariables1.getTaskId();
        Response result1 = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );

        String taskId2 = taskVariables2.getTaskId();
        Response result2 = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result2.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        common.setupOnlyCaseManagerForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables1.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result1 = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );
        result2 = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        result2.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        assertions.taskVariableWasUpdated(taskVariables1.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId1, List.of("completed", "terminated"),
                                                 waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
    }


}

