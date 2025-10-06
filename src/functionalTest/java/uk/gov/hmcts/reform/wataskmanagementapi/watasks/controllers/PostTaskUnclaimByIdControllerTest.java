package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.REGION;


public class PostTaskUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";


    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(unassignUser.getHeaders());
        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        TestVariables taskVariables = setupScenario("processApplication",
            "process application");
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        Response result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "task-supervisor");
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "cftTaskState", "unassigned");
        assertions.taskStateWasUpdatedInDatabase(taskId, "unassigned", waCaseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", null, waCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario("processApplication",
            "process application");

        initiateTask(taskVariables);

        common.updateTaskWithCustomVariablesOverride(taskVariables, Map.of(REGION, "1"));

        common.setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );

        String taskId = taskVariables.getTaskId();
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
    public void should_return_a_204_when_unclaiming_a_task_by_id_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");

        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "senior-tribunal-caseworker");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            unassignUser.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            unassignUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unassigning_a_task_by_id_with_different_user_credentials_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");

        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "task-supervisor");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            unassignUser.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_unassign_a_task_different_user_without_un_assign_permission_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(),
            "WA", "WaCaseType");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "senior-tribunal-caseworker");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            waCaseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            unassignUser.getHeaders()
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

    private TestVariables setupScenario(String taskType, String taskName) {
        TestVariables taskVariables
            = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
            taskType,
            taskName);
        common.setupCFTOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE);
        return taskVariables;
    }

}
