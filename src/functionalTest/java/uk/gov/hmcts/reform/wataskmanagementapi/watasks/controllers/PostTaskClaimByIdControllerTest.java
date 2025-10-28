package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.ROLE_ASSIGNMENT_VERIFICATION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_CASEWORKER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;


@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskClaimByIdControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/claim";

    TestAuthenticationCredentials caseWorkerWithTribRole;

    @Before
    public void setUp() {
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_TRIB_CASEWORKER_ROLE);
    }

    @Test
    public void user_should_not_claim_task_when_role_assignment_verification_failed() {

        TestAuthenticationCredentials caseWorkerWithLeadJudgeSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            caseWorkerWithLeadJudgeSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithLeadJudgeSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .and()
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithLeadJudgeSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithLeadJudgeSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("processApplication", "Process Application");

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_409_when_claiming_a_task_that_was_already_claimed() {

        TestAuthenticationCredentials caseMngrWithSpAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication", "Process Application");

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseMngrWithSpAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TestAuthenticationCredentials caseMngrWithSpAccess2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(
            caseMngrWithSpAccess2.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseMngrWithSpAccess2.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CONFLICT.value());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseMngrWithSpAccess2.getHeaders());
        authorizationProvider.deleteAccount(caseMngrWithSpAccess2.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

}

