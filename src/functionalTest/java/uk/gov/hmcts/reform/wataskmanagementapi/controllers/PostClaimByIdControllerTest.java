package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class PostClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                nonExistentTaskId
            )));
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

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
    public void should_return_a_204_when_claiming_a_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_claiming_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void endpoint_should_be_idempotent_should_return_a_204_when_claiming_a_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        Response resultAfterClaimedBySameUser = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultAfterClaimedBySameUser.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
        common.setupOrganisationalRoleAssignment(otherUser.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUser.getHeaders()
        );


        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value())
            .and()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.CONFLICT.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.CONFLICT.value()))
            .body("message", equalTo(String.format(
                "Task '%s' is already claimed by someone else.",
                taskId
            )));

        common.cleanUpTask(taskId);
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_jurisdiction_did_not_match() {
        TestVariables taskVariables = common.setupTaskWithoutCcdCaseAndRetrieveIdsWithCustomVariable(
            JURISDICTION, "SSCS"
        );
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

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
    public void should_return_a_204_and_claim_a_task_by_id_jurisdiction_location_and_region_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(REGION, "1");
        String taskId = taskVariables.getTaskId();


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
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

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_the_role_base_location_does_not_match_to_the_task_location() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(LOCATION, "765324");
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "baseLocation", "698118",
                "jurisdiction", "IA",
                "region", "1"
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

}

