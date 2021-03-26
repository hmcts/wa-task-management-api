package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        common.clearAllRoleAssignments(authenticationHeaders);
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(String.format(
                "There was a problem fetching the variables for task with id: %s",
                nonExistentTaskId
            )));
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))

            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_different_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");
        String taskId = taskVariables.getTaskId();

        Headers otherUserAuthenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerBAuthorization();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        common.setupOrganisationalRoleAssignment(otherUserAuthenticationHeaders);

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUserAuthenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        common.updateTaskWithCustomVariablesOverride(taskVariables, Map.of(REGION, "1"));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "2"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message", equalTo(
                format("User did not have sufficient permissions to unclaim task with id: %s", taskId)
            ));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    private TestVariables setupScenario() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );

        return taskVariables;
    }

}

