package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.REGION;


public class PostTaskUnclaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    TestAuthenticationCredentials waCaseworkerWithNoRoles;
    TestAuthenticationCredentials userWithTaskSupervisorRole;
    TestAuthenticationCredentials caseWorkerWithCftOrgRoles;
    TestAuthenticationCredentials userWithSeniorTribCaseworker;

    @Before
    public void setUp() {
        waCaseworkerWithNoRoles = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_NO_ROLES);
        userWithTaskSupervisorRole = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE);
        caseWorkerWithCftOrgRoles = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_CFT_ORG_ROLES);
        userWithSeniorTribCaseworker = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_SENIOR_TRIB_ROLE);

    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        TestAuthenticationCredentials caseWorkerForClaim =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            caseWorkerForClaim.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE);

        TestVariables taskVariables = setupScenario("processApplication",
            "process application");
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            CLAIM_ENDPOINT,
            taskId,
            caseWorkerForClaim.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithTaskSupervisorRole.getHeaders()

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions()
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "cftTaskState", "unassigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskId, "unassigned", userWithTaskSupervisorRole.getHeaders());
        taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
            taskId, "assignee", null, userWithTaskSupervisorRole.getHeaders());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerForClaim.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerForClaim.getAccount().getUsername());
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario("processApplication",
            "process application");

        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().updateTaskWithCustomVariablesOverride(
            taskVariables, Map.of(REGION, "1"));

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            caseWorkerWithCftOrgRoles.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA",
                "region", "2"
            )
        );

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
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
    public void should_return_a_204_when_unclaiming_a_task_by_id_gp_flag_on() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            userWithSeniorTribCaseworker.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithSeniorTribCaseworker.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions()
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unassigning_a_task_by_id_with_different_user_credentials_gp_flag_on() {
        TestAuthenticationCredentials caseWorkerForClaim =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            caseWorkerForClaim.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerForClaim.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithTaskSupervisorRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions()
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerForClaim.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerForClaim.getAccount().getUsername());
    }

    @Test
    public void should_return_a_403_when_unassign_a_task_different_user_without_un_assign_permission_gp_flag_on() {
        TestAuthenticationCredentials caseWorkerForClaim =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            caseWorkerForClaim.getHeaders(), "WA", "WaCaseType");

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerForClaim.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            userWithSeniorTribCaseworker.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerForClaim.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerForClaim.getAccount().getUsername());
    }

    private TestVariables setupScenario(String taskType, String taskName) {
        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data.json",
            taskType,
            taskName);
        return taskVariables;
    }

}
