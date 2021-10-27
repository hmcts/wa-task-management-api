package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class PostTaskAssignByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";
    private Headers authenticationHeaders;
    private String assigneeId;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
        assigneeId = getAssigneeId(authenticationHeaders);
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        boolean isFeatureEnabled = launchDarklyClient.getKey(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE.getKey(),
            authenticationHeaders.getValue(AUTHORIZATION));

        if (isFeatureEnabled) {
            String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

            common.setupOrganisationalRoleAssignment(authenticationHeaders);

            Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                nonExistentTaskId,
                new AssignTaskRequest(assigneeId),
                authenticationHeaders
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
    }

    @Test
    public void should_return_a_204_when_assigning_a_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_assigning_a_task_by_id_with_restricted_role_assignment() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), authenticationHeaders);
        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
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

    @Disabled("Disabled temporarily see RWA-858")
    @Test
    public void should_return_a_403_when_the_assigner_does_not_have_manage_permission() {

        String noManagePermission = "Read,Refer,Own,Cancel";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.overrideTaskPermissions(taskId, noManagePermission);
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"))
            .body("title", equalTo("Role Assignment Verification"))
            .body("status", equalTo(403))
            .body("detail", equalTo(
                "Role Assignment Verification: The request failed the Role Assignment checks performed."));

        common.cleanUpTask(taskId);
    }

    @Disabled("Disabled temporarily see RWA-858")
    @Test
    public void should_return_a_403_when_the_assignee_does_not_have_execute_or_own_permissions() {

        String noOwnPermission = "Read,Refer,Manage,Cancel";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.overrideTaskPermissions(taskId, noOwnPermission);
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        initiateTask(taskVariables);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"))
            .body("title", equalTo("Role Assignment Verification"))
            .body("status", equalTo(403))
            .body("detail", equalTo(
                "Role Assignment Verification: The request failed the Role Assignment checks performed."));

        common.cleanUpTask(taskId);
    }

    private String getAssigneeId(Headers headers) {
        return authorizationHeadersProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }

    private void initiateTask(TestVariables testVariables) {

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_CASE_ID, testVariables.getCaseId()),
            new TaskAttribute(TASK_TYPE, "reviewTheAppeal"),
            new TaskAttribute(TASK_NAME, "Review The Appeal")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            testVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(testVariables.getTaskId()))
            .body("case_id", equalTo(testVariables.getCaseId()));
    }

}

