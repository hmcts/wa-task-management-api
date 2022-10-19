package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

public class PostTaskUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials currentCaseworkerCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;
    private GrantType testGrantType = GrantType.SPECIFIC;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        currentCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        caseworkerForReadCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
        granularPermissionCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-");
    }

    @After
    public void cleanUp() {
        if (testGrantType == GrantType.CHALLENGED) {
            common.clearAllRoleAssignmentsForChallenged(caseworkerCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(currentCaseworkerCredentials.getHeaders());
            common.clearAllRoleAssignmentsForChallenged(granularPermissionCaseworkerCredentials.getHeaders());
        } else {
            common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
            common.clearAllRoleAssignments(currentCaseworkerCredentials.getHeaders());
            common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());
        }
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());

        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(currentCaseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials unassignUser =
            authorizationProvider.getNewTribunalCaseworker("wa-granular-permission-");

        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "task-supervisor");

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

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        assertions.taskStateWasUpdatedInDatabase(taskId, "unassigned", caseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", null, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unassigning_a_task_by_id_with_different_user_credentials_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials unassignUser =
            authorizationProvider.getNewTribunalCaseworker("wa-granular-permission-");

        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "task-supervisor");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
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
        assertions.taskStateWasUpdatedInDatabase(taskId, "unassigned", caseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", null, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_unassigning_a_task_by_id_with_different_user_withoutun_assign_permission_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(),
                                                    "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials unassignUser =
            authorizationProvider.getNewTribunalCaseworker("wa-granular-permission-");
        common.setupWAOrganisationalRoleAssignment(unassignUser.getHeaders(), "senior-tribunal-caseworker");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
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

}
