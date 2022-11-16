package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TestAssertionsBuilder.buildTaskActionAttributesForAssertion;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskCompleteByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/complete";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";
    private static final String ASSIGN_ENDPOINT = "task/{task-id}/assign";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private String assigneeId;
    private String taskId;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2-");
        caseworkerForReadCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2-");
        assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());

        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_when_completing_a_task_by_id() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.WA);

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        Map<String, Matcher<?>> valueMap = buildTaskActionAttributesForAssertion(taskId, userInfo.getUid(),
            "assigned", userInfo.getUid(), CLAIM);
        assertions.taskAttributesVerifier(taskId, valueMap, caseworkerCredentials.getHeaders());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "completed", userInfo.getUid(), COMPLETED);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_403_if_task_was_not_previously_assigned() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, Jurisdiction.WA);
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

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, null,
            "unassigned", idamSystemUser, CONFIGURE);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());
    }

    @Test
    public void should_succeed_and_return_204_when_a_task_that_was_already_claimed_and_privileged_auto_complete() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, Jurisdiction.WA);
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
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

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "completed", userInfo.getUid(), COMPLETED);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(otherUser.getHeaders());

    }

    @Test
    public void should_not_complete_when_a_task_was_already_claimed_and_privileged_auto_complete_is_false() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, Jurisdiction.WA);
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
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

        UserInfo userInfo = idamService.getUserInfo(caseworkerCredentials.getHeaders().getValue(AUTHORIZATION));

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
        common.clearAllRoleAssignments(otherUser.getHeaders());

    }

    @Test
    public void should_return_a_204_when_completing_a_task_with_completion_options_assign_and_complete_true() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, Jurisdiction.WA);
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
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

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "completed", userInfo.getUid(), COMPLETED);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void user_should_complete_a_assigned_task() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.WA);

        assignTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "completed", userInfo.getUid(), COMPLETED);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    //Add four IT to cover grant type SPECIFIC, STANDARD, CHALLENGED, EXCLUDED for complete request, then remove this.
    @Test
    public void should_return_a_204_when_completing_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication");
        taskId = taskVariables.getTaskId();
        initiateTask(taskVariables, Jurisdiction.WA);

        common.setupRestrictedRoleAssignmentForWA(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "completed", userInfo.getUid(), COMPLETED);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "completed");


        common.cleanUpTask(taskId);
    }

    //Need new IT to cover role assignment verification for attributes in common for all actions, then remove this test.
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIdsWithCustomVariable(REGION, "1", "requests/ccd/wa_case_data.json");
        taskId = taskVariables.getTaskId();

        common.setupRestrictedRoleAssignmentForWA(taskVariables.getCaseId(), caseworkerForReadCredentials.getHeaders());
        initiateTask(taskVariables, caseworkerForReadCredentials.getHeaders());
        //Create temporary role-assignment to assign task
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.FORBIDDEN
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

        //This need to be changed when assign endpoint story RWA-1582 is played
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, assigneeId,
            "assigned", idamSystemUser, CONFIGURE);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

    }


}