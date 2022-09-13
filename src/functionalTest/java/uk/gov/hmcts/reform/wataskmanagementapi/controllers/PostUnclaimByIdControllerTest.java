package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class PostUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";

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
                LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
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
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId);
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_unclaiming_a_task_by_id_with_different_tribunal_caseworker_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");
        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        common.setupOrganisationalRoleAssignment(otherUser.getHeaders(), "tribunal-caseworker");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUser.getHeaders()
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
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_different_senior_tcw_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");
        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        common.setupOrganisationalRoleAssignment(otherUser.getHeaders(), "senior-tribunal-caseworker");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(otherUser.getHeaders());
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.updateTaskWithCustomVariablesOverride(taskVariables, Map.of(REGION, "1"));

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

    private TestVariables setupScenario() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders()
        );

        return taskVariables;
    }

}

