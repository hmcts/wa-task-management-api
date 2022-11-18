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

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TestAssertionsBuilder.buildTaskActionAttributesForAssertion;


@SuppressWarnings("checkstyle:LineLength")
public class PostTaskClaimByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials currentCaseworkerCredentials;
    private TestAuthenticationCredentials caseworkerForReadCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;

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
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(currentCaseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(caseworkerForReadCredentials.getHeaders());

        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(currentCaseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(caseworkerForReadCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_not_claim_task_when_role_assignment_verification_failed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        Map<String, Matcher<?>> valueMap = buildTaskActionAttributesForAssertion(taskId, null,
            "unassigned", idamSystemUser, CONFIGURE);
        assertions.taskAttributesVerifier(taskId, valueMap, caseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);

    }

    @Test
    public void user_should_claim_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");

        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String serviceToken = caseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, userInfo.getUid(),
            "assigned", userInfo.getUid(), CLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerCredentials.getHeaders());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");

        common.setupCaseManagerForSpecificAccess(currentCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            currentCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCaseManagerForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
                                                 WA_JURISDICTION, WA_CASE_TYPE);

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        String serviceToken = currentCaseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, userInfo.getUid(),
            "assigned", userInfo.getUid(), CLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, currentCaseworkerCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_when_granular_permission_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(granularPermissionCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String serviceToken = granularPermissionCaseworkerCredentials.getHeaders().getValue(AUTHORIZATION);
        UserInfo userInfo = authorizationProvider.getUserInfo(serviceToken);

        common.setupCFTOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE);
        Map<String, Matcher<?>> taskValueMap = buildTaskActionAttributesForAssertion(taskId, userInfo.getUid(),
            "assigned", userInfo.getUid(), CLAIM);
        assertions.taskAttributesVerifier(taskId, taskValueMap, caseworkerForReadCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_not_claim_a_task_when_granular_permission_not_satisfied() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        String taskId = taskVariables.getTaskId();

        common.setupStandardCaseManager(granularPermissionCaseworkerCredentials.getHeaders(),
                                        taskVariables.getCaseId(), "WA", "WaCaseType");

        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        Map<String, Matcher<?>> valueMap = buildTaskActionAttributesForAssertion(taskId, null,
            "unassigned", idamSystemUser, CONFIGURE);
        common.setupCFTOrganisationalRoleAssignment(caseworkerForReadCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE);
        assertions.taskAttributesVerifier(taskId, valueMap, caseworkerForReadCredentials.getHeaders());

        common.cleanUpTask(taskId);
    }

}

