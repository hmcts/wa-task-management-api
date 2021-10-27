package uk.gov.hmcts.reform.wataskmanagementapi.controllers;


import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.REGION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class PostUnclaimByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/unclaim";
    private static final String CLAIM_ENDPOINT = "task/{task-id}/claim";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider
            .getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
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
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_TASK_WITH_ID,
                nonExistentTaskId
            )));
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
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

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, "2021-10-27T13:26:46+0100"),
            new TaskAttribute(TASK_DUE_DATE, "2021-10-29T13:26:46+0100")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());


        result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_restricted_role_assignment() {
        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, "2021-10-27T13:26:46+0100"),
            new TaskAttribute(TASK_DUE_DATE, "2021-10-29T13:26:46+0100")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
        result = restApiActions.post(
            CLAIM_ENDPOINT,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), authenticationHeaders);

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders

        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions
            .taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "unassigned");

        common.cleanUpTask(taskId);
    }

    @Ignore
    @Disabled("Disabled temporarily see RWA-858")
    @Test
    public void should_return_a_403_when_unclaiming_a_task_by_id_with_different_tribunal_caseworker_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");
        String taskId = taskVariables.getTaskId();
        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, "2021-10-27T13:26:46+0100"),
            new TaskAttribute(TASK_DUE_DATE, "2021-10-29T13:26:46+0100")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        Headers otherUserAuthenticationHeaders = authorizationHeadersProvider
            .getTribunalCaseworkerBAuthorization("wa-ft-test-r2-");
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        common.setupOrganisationalRoleAssignment(otherUserAuthenticationHeaders, "tribunal-caseworker");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUserAuthenticationHeaders
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

    @Test
    public void should_return_a_204_when_unclaiming_a_task_by_id_with_different_senior_tcw_credentials() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(ASSIGNEE, "random_uid");
        String taskId = taskVariables.getTaskId();

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, "2021-10-27T13:26:46+0100"),
            new TaskAttribute(TASK_DUE_DATE, "2021-10-29T13:26:46+0100")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
        Headers otherUserAuthenticationHeaders = authorizationHeadersProvider
            .getTribunalCaseworkerBAuthorization("wa-ft-test-r2-");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        common.setupOrganisationalRoleAssignment(otherUserAuthenticationHeaders, "senior-tribunal-caseworker");

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            authenticationHeaders
        );

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            otherUserAuthenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    @Ignore
    @Disabled("Disabled temporarily see RWA-858")
    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = setupScenario();
        String taskId = taskVariables.getTaskId();
        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, "2021-10-27T13:26:46+0100"),
            new TaskAttribute(TASK_DUE_DATE, "2021-10-29T13:26:46+0100")
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        common.updateTaskWithCustomVariablesOverride(taskVariables, Map.of(REGION, "1"));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "2"
            )
        );

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
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

    private TestVariables setupScenario() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        return taskVariables;
    }

}
