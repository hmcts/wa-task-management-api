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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

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

public class PostUnclaimByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {

        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));

    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds("followUpOverdueReasonsForAppeal");
        String taskId = taskVariables.getTaskId();

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

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
        TestVariables taskVariables = setupScenario("followUpOverdueReasonsForAppeal");
        String taskId = taskVariables.getTaskId();

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "task-supervisor");
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        assertions.taskStateWasUpdatedInDatabase(taskId, "unassigned", caseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", null, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = setupScenario("followUpOverdueReasonsForAppeal");
        String taskId = taskVariables.getTaskId();

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        assertions.taskStateWasUpdatedInDatabase(taskId, "unassigned", caseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", null, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_403_when_unclaiming_a_task_by_id_with_different_tribunal_caseworker_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        common.setupCFTOrganisationalRoleAssignment(otherUser.getHeaders(), "tribunal-caseworker");
        String taskId = taskVariables.getTaskId();
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.FORBIDDEN
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

    @Ignore
    @Test
    public void should_return_a_forbidden_when_unclaiming_a_task_by_id_with_different_tcw_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

        TestAuthenticationCredentials otherUser =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        common.setupOrganisationalRoleAssignment(otherUser.getHeaders());
        String taskId = taskVariables.getTaskId();

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value());

        common.cleanUpTask(taskId);
    }


    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario("followUpOverdueReasonsForAppeal");

        initiateTaskAttributes(taskVariables, Jurisdiction.IA);

        common.updateTaskWithCustomVariablesOverride(taskVariables, Map.of(REGION, "1"));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "2"
            )
        );

        String taskId = taskVariables.getTaskId();
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

    private TestVariables setupScenario(String taskType) {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        return taskVariables;
    }

}
