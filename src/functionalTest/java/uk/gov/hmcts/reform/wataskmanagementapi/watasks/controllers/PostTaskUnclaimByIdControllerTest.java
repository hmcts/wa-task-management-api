package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TestAssertionsBuilder.buildTaskActionAttributesForAssertion;

public class PostTaskUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";

    private TestAuthenticationCredentials caseworkerCredentials;

    private TestAuthenticationCredentials unassignUser;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        unassignUser =
            authorizationProvider.getNewTribunalCaseworker("wa-granular-permission-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
            "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

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

        String serviceToken = unassignUser.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, null,
            "unassigned", userInfo.getUid(), UNCLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, unassignUser.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unassigning_a_task_by_id_with_different_user_credentials_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
            "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();

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

        String serviceToken = unassignUser.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, null,
            "unassigned", userInfo.getUid(), UNCLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, unassignUser.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_unassign_a_task_different_user_without_un_assign_permission_gp_flag_on() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json", "processApplication");

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(),
            "WA", "WaCaseType");

        initiateTask(taskVariables, Jurisdiction.WA);

        String taskId = taskVariables.getTaskId();

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

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, userInfo.getUid(),
            "assigned", userInfo.getUid(), CLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

}
