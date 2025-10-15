package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
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

    private String assigneeId;
    private String taskId;

    TestAuthenticationCredentials waCaseworkerCredentials;
    TestAuthenticationCredentials caseworkerForReadCredentials;
    TestAuthenticationCredentials otherUser;
    TestAuthenticationCredentials caseworkerCredentials;
    TestAuthenticationCredentials waCaseWorkerCompletionEnabled;
    TestAuthenticationCredentials waCaseWorkerCompletionDisabled;

    @Before
    public void setUp() {
        waCaseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(TaskFunctionalTestsUserUtils.WA_CASE_WORKER);
        caseworkerForReadCredentials = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.CASE_WORKER_FOR_READ);
        otherUser = taskFunctionalTestsUserUtils.getTestUser(TaskFunctionalTestsUserUtils.OTHER_USER);
        caseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER);
        waCaseWorkerCompletionEnabled = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.WA_USER_COMPLETION_ENABLED);
        waCaseWorkerCompletionDisabled = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.WA_USER_COMPLETION_DISABLED);

        assigneeId = getAssigneeId(waCaseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(otherUser.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorkerCompletionEnabled.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorkerCompletionDisabled.getHeaders());
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_termination_process() {
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseWorkerCompletionEnabled.getHeaders());
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
                taskId, "termination_process", null, waCaseWorkerCompletionEnabled.getHeaders()
            );
            Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                CLAIM_ENDPOINT,
                taskId,
                waCaseWorkerCompletionEnabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, waCaseWorkerCompletionEnabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                waCaseWorkerCompletionEnabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", terminationProcess, waCaseWorkerCompletionEnabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
                taskVariables.getProcessInstanceId(), "taskState", "completed");
            taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
                taskId, "completed", waCaseWorkerCompletionEnabled.getHeaders());

            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        }
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorkerCompletionEnabled.getHeaders());
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id_and_null_termination_process_when_flag_disabled() {
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseWorkerCompletionDisabled.getHeaders());
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
                taskId, "termination_process", null, waCaseWorkerCompletionDisabled.getHeaders()
            );
            Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                CLAIM_ENDPOINT,
                taskId,
                waCaseWorkerCompletionDisabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, waCaseWorkerCompletionDisabled.getHeaders()
            );
            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
            String completionProcess = data[0];
            String terminationProcess = data[1];
            result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED + "?completion_process=" + completionProcess,
                taskId,
                waCaseWorkerCompletionDisabled.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());

            taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
                taskId, "termination_process", null, waCaseWorkerCompletionDisabled.getHeaders()
            );
            taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
                taskVariables.getProcessInstanceId(), "taskState", "completed");
            taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
                taskId, "completed", waCaseWorkerCompletionDisabled.getHeaders());

            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        }
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseWorkerCompletionDisabled.getHeaders());
    }

    @Test
    public void should_return_a_403_if_task_was_not_previously_assigned() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(
            new CompletionOptions(false));
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_not_complete_when_a_task_was_already_claimed_and_privileged_auto_complete_is_false() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        //S2S service name is wa_task_management_api
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(
            new CompletionOptions(false));
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_assign_and_complete_true() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_after_assign_complete_terminate_true() {

        TestVariables taskVariables =  taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", waCaseworkerCredentials.getHeaders());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response deleteResult = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            "/task/{task-id}",
            taskId,
            terminateTaskRequest,
            waCaseworkerCredentials.getHeaders()
        );

        deleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "terminated", waCaseworkerCredentials.getHeaders());

        Response reCompleteResult =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(false)),
            waCaseworkerCredentials.getHeaders()
        );

        reCompleteResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "terminated", waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    @Test
    public void user_should_complete_a_assigned_task() {

        TestVariables taskVariables =  taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        assignTask(taskVariables);

        Response result =  taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for complete request, then remove this.
    @Test
    public void should_return_a_204_when_completing_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupSpecificTribunalCaseWorker(
            taskVariables.getCaseId(), waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "completed", waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    //Need new IT to cover role assignment verification for attributes in common for all actions, then remove this test.
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithWithCustomVariableAndRetrieveIds(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());
        //Create temporary role-assignment to assign task
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.FORBIDDEN
        );

        //Delete role-assignment and re-create
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );


        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_user_does_not_have_permission() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");

        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "case-manager");
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "case-manager");

        String taskId = taskVariables.getTaskId();

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    private void assignTask(TestVariables taskVariables) {

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ASSIGN_ENDPOINT,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", waCaseworkerCredentials.getHeaders()
        );
        taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee", assigneeId, waCaseworkerCredentials.getHeaders()
        );

    }

    @Test
    public void should_return_a_case_role_assignment() {
        TestVariables taskVariables1 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables1);
        TestVariables taskVariables2 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("processApplication", "Process Application");
        initiateTask(taskVariables2);

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        String taskId1 = taskVariables1.getTaskId();
        Response result1 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );

        String taskId2 = taskVariables2.getTaskId();
        Response result2 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result2.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().setupOnlyCaseManagerForSpecificAccess(waCaseworkerCredentials.getHeaders(), taskVariables1.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result1 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );
        result2 = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        result2.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(taskVariables1.getProcessInstanceId(), "taskState", "completed");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(taskId1, List.of("completed", "terminated"),
                                                                                  waCaseworkerCredentials.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId1);
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId2);
    }


}

