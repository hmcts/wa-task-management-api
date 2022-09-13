package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCompleteByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/complete";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";
    private static final String ASSIGN_ENDPOINT = "task/{task-id}/assign";

    private TestAuthenticationCredentials caseworkerCredentials;
    private String assigneeId;
    private String taskId;
    private GrantType testGrantType = GrantType.SPECIFIC;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2-");
        assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        if (testGrantType == GrantType.CHALLENGED) {
            common.clearAllRoleAssignmentsForChallenged(caseworkerCredentials.getHeaders());
        } else {
            common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        }
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {

        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type",
                equalTo("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));

    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_403_if_task_was_not_previously_assigned() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
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
    public void should_return_a_204_when_completing_a_task_by_id_with_restricted_role_assignment() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        common.setupRestrictedRoleAssignmentForWA(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_and_retrieve_a_task_by_id_jurisdiction_location_and_region_match() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIdsWithCustomVariable(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        //Create temporary role-assignment to assign task
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        //Delete role-assignment and re-create
        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );


        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
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
    public void should_succeed_and_return_204_when_a_task_that_was_already_claimed_and_privileged_auto_complete() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        //S2S service name is wa_task_management_api
        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        common.setupCFTOrganisationalRoleAssignmentForWA(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(new CompletionOptions(true));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            otherUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(otherUser.getHeaders());

    }

    @Test
    public void should_complete_when_a_task_was_already_claimed_and_privileged_auto_complete_is_false() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        //S2S service name is wa_task_management_api
        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        common.setupCFTOrganisationalRoleAssignmentForWA(otherUser.getHeaders());

        CompleteTaskRequest completeTaskRequest = new CompleteTaskRequest(new CompletionOptions(false));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            completeTaskRequest,
            otherUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(otherUser.getHeaders());

    }

    @Test
    public void should_return_a_404_if_task_does_not_exist_with_completion_options_assign_and_complete_true() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type",
                equalTo("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_if_task_was_not_previously_assigned_and_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_restricted_role_assignment_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupRestrictedRoleAssignmentForWA(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles_and_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_204_and_retrieve_task_role_assignment_attributes_match_and_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_403_when_permission_region_did_not_match_and_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIdsWithCustomVariable(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");
        //Create temporary role-assignment to assign task
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        //Delete role-assignment and re-create
        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );


        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new CompleteTaskRequest(new CompletionOptions(true)),
            caseworkerCredentials.getHeaders()
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
    public void user_should_complete_task() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        assignTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");
        assertions.taskStateWasUpdatedInDatabase(taskId, "completed", caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void user_should_not_complete_task_when_grant_type_specific_and_permission_read() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        assignTask(taskVariables);

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);

    }

    @Test
    public void user_should_not_complete_task_when_grant_type_challenged_and_permission_read() {
        testGrantType = GrantType.CHALLENGED;
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        assignTask(taskVariables);

        common.setupChallengedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);

    }

    private void assignTask(TestVariables taskVariables) {

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);

        Response result = restApiActions.post(
            ASSIGN_ENDPOINT,
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", caseworkerCredentials.getHeaders()
        );
        assertions.taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee", assigneeId, caseworkerCredentials.getHeaders()
        );

    }


}

