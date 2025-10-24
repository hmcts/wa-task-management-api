package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

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
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_COMPLETION_DISABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_COMPLETION_ENABLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_WA_ORG_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_WA_ORG_ROLES2;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.USER_WITH_WA_ORG_ROLES3;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCompleteByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    protected IdamService idamService;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/complete";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";
    private static final String ASSIGN_ENDPOINT = "task/{task-id}/assign";

    private String taskId;

    TestAuthenticationCredentials caseWorkerWithWAOrgRoles;
    TestAuthenticationCredentials caseWorkerWithWAOrgRoles2;
    TestAuthenticationCredentials caseWorkerWithWAOrgRoles3;
    TestAuthenticationCredentials caseWorkerWithCompletionEnabled;
    TestAuthenticationCredentials caseWorkerWithCompletionDisabled;
    TestAuthenticationCredentials caseWorkerWithCftOrgRoles;
    TestAuthenticationCredentials userWithCaseManagerRole;
    TestAuthenticationCredentials userWithCaseManagerRole2;

    @Before
    public void setUp() {
        caseWorkerWithWAOrgRoles = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_WA_ORG_ROLES);
        caseWorkerWithWAOrgRoles2 = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_WA_ORG_ROLES2);
        caseWorkerWithWAOrgRoles3 = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_WA_ORG_ROLES3);
        caseWorkerWithCompletionEnabled = taskFunctionalTestsUserUtils.getTestUser(
            USER_WITH_COMPLETION_ENABLED);
        caseWorkerWithCompletionDisabled = taskFunctionalTestsUserUtils.getTestUser(
            USER_WITH_COMPLETION_DISABLED);
        caseWorkerWithCftOrgRoles = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.USER_WITH_CFT_ORG_ROLES);
        userWithCaseManagerRole = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_CASE_MANAGER_ROLE);
        userWithCaseManagerRole2 = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_CASE_MANAGER_ROLE2);
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", caseWorkerWithWAOrgRoles2.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_termination_process() {
        String[][] testData = {
            {"EXUI_USER_COMPLETION", "EXUI_USER_COMPLETION"},
            {"EXUI_CASE-EVENT_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        for (String[] data : testData) {

            TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
            taskId = taskVariables.getTaskId();
            initiateTask(taskVariables);
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, caseWorkerWithCompletionEnabled.getHeaders()
            );
            Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                CLAIM_ENDPOINT,
                taskId,
                caseWorkerWithCompletionEnabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, caseWorkerWithCompletionEnabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                caseWorkerWithCompletionEnabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", terminationProcess, caseWorkerWithCompletionEnabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
                taskVariables.getProcessInstanceId(), "taskState", "completed");
            taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
                taskId, "completed", caseWorkerWithCompletionEnabled.getHeaders());

            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        }
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_null_termination_process_when_flag_disabled() {
        String[][] testData = {
            {"EXUI_USER_COMPLETION", "EXUI_USER_COMPLETION"},
            {"EXUI_CASE-EVENT_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"},
            {"INVALID_VALUE", null},
            {null, null},
            {"", null}
        };

        for (String[] data : testData) {

            TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
            taskId = taskVariables.getTaskId();

            initiateTask(taskVariables);
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, caseWorkerWithCompletionDisabled.getHeaders()
            );
            Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                CLAIM_ENDPOINT,
                taskId,
                caseWorkerWithCompletionDisabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, caseWorkerWithCompletionDisabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                caseWorkerWithCompletionDisabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, caseWorkerWithCompletionDisabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
                taskVariables.getProcessInstanceId(), "taskState", "completed");
            taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
                taskId, "completed", caseWorkerWithCompletionDisabled.getHeaders());

            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        }
    }

    @Test
    public void should_return_a_403_if_task_was_not_previously_assigned() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders()
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

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(
            new CompletionOptions(false));
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            caseWorkerWithWAOrgRoles3.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(caseWorkerWithWAOrgRoles2.getHeaders().getValue(AUTHORIZATION));

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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_not_complete_when_a_task_was_already_claimed_and_privileged_auto_complete_is_false() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(
            new CompletionOptions(false));
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            caseWorkerWithWAOrgRoles3.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(caseWorkerWithWAOrgRoles2.getHeaders().getValue(AUTHORIZATION));

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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_assign_and_complete_true() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", caseWorkerWithWAOrgRoles2.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_after_assign_complete_terminate_true() {

        TestVariables taskVariables =  taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithWAOrgRoles2.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", caseWorkerWithWAOrgRoles2.getHeaders());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response deleteResult = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            "/task/{task-id}",
            taskId,
            terminateTaskRequest,
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        deleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "terminated", caseWorkerWithWAOrgRoles2.getHeaders());

        Response reCompleteResult =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(false)),
            caseWorkerWithWAOrgRoles2.getHeaders()
        );

        reCompleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "terminated", caseWorkerWithWAOrgRoles2.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void user_should_complete_a_assigned_task() {

        TestVariables taskVariables =  taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        assignTask(taskVariables);

        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", caseWorkerWithWAOrgRoles3.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for complete request, then remove this.
    @Test
    public void should_return_a_204_when_completing_a_task_by_id_with_restricted_role_assignment() {
        TestAuthenticationCredentials userWithSpecificTribunalCaseWorker =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupSpecificTribunalCaseWorker(
            taskVariables.getCaseId(), userWithSpecificTribunalCaseWorker.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            userWithSpecificTribunalCaseWorker.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithSpecificTribunalCaseWorker.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", userWithSpecificTribunalCaseWorker.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(userWithSpecificTribunalCaseWorker.getHeaders());
        authorizationProvider.deleteAccount(userWithSpecificTribunalCaseWorker.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    //Need new IT to cover role assignment verification for attributes in common for all actions, then remove this test.
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithWithCustomVariableAndRetrieveIds(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        initiateTask(taskVariables, caseWorkerWithCftOrgRoles.getHeaders());
        //Create temporary role-assignment to assign task

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders(),
            HttpStatus.FORBIDDEN
        );

        //Delete role-assignment and re-create
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            caseWorkerWithWAOrgRoles.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );


        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_user_does_not_have_permission() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId,
            userWithCaseManagerRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithCaseManagerRole2.getHeaders()
        );

        UserInfo userInfo = idamService.getUserInfo(userWithCaseManagerRole.getHeaders().getValue(AUTHORIZATION));

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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    private void assignTask(TestVariables taskVariables) {

        String assigneeId = getAssigneeId(caseWorkerWithWAOrgRoles3.getHeaders());

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ASSIGN_ENDPOINT,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", caseWorkerWithCftOrgRoles.getHeaders()
        );
        taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee", assigneeId, caseWorkerWithCftOrgRoles.getHeaders()
        );

    }

    @Test
    public void should_return_a_case_role_assignment() {

        TestAuthenticationCredentials waCaseWorker =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables1 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables1);
        TestVariables taskVariables2 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables2);

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(waCaseWorker.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        String taskId1 = taskVariables1.getTaskId();
        Response result1 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId1,
            waCaseWorker.getHeaders()
        );

        String taskId2 = taskVariables2.getTaskId();
        Response result2 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId2,
            waCaseWorker.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result2.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorker.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().setupOnlyCaseManagerForSpecificAccess(waCaseWorker.getHeaders(), taskVariables1.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result1 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId1,
            waCaseWorker.getHeaders()
        );
        result2 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId2,
            waCaseWorker.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        result2.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(waCaseWorker.getHeaders());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(taskVariables1.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(taskId1, List.of("completed", "terminated"),
                                                                                  waCaseWorker.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorker.getHeaders());
        authorizationProvider.deleteAccount(waCaseWorker.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId1);
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId2);
    }


}

