package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class PostTaskInitiateByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-");
        common.clearAllRoleAssignments(authenticationHeaders);
    }

    @Test
    public void should_return_a_201_when_initiating_a_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("aTaskName"))
            .body("task_type", equalTo("aTaskType"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("aTaskName"))
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("Asylum"))
            .body("case_name", equalTo("Bob Smith"))
            .body("case_category", equalTo("Protection"))
            .body("jurisdiction", equalTo("IA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body("execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                        + "(Typically this will be in CCD.)"))
            .body("task_role_resources[0].task_role_id", notNullValue())
            .body("task_role_resources[0].task_id", equalTo(taskId))
            .body("task_role_resources[0].role_name",
                anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker")))
            .body("task_role_resources[0].read", equalTo(true))
            .body("task_role_resources[0].own", equalTo(true))
            .body("task_role_resources[0].execute", equalTo(false))
            .body("task_role_resources[0].cancel", equalTo(true))
            .body("task_role_resources[0].refer", equalTo(true))
            .body("task_role_resources[0].authorizations", equalTo(emptyList()))
            .body("task_role_resources[0].auto_assignable", equalTo(false))
            .body("task_role_resources[0].role_category",
                anyOf(is("LEGAL_OPERATIONS"), is(nullValue())))
            .body("task_role_resources[1].task_role_id", notNullValue())
            .body("task_role_resources[1].task_id", equalTo(taskId))
            .body("task_role_resources[1].role_name",
                anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker")))
            .body("task_role_resources[1].read", equalTo(true))
            .body("task_role_resources[1].own", equalTo(true))
            .body("task_role_resources[1].execute", equalTo(false))
            .body("task_role_resources[1].cancel", equalTo(true))
            .body("task_role_resources[1].refer", equalTo(true))
            .body("task_role_resources[1].authorizations", equalTo(emptyList()))
            .body("task_role_resources[1].auto_assignable", equalTo(false))
            .body("task_role_resources[1].role_category",
                anyOf(is("LEGAL_OPERATIONS"), is(nullValue())));


        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned");

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_503_if_task_already_initiated() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));
        //First call
        Response resultFirstCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        resultFirstCall.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        //Second call
        Response resultSecondCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        // If the first call succeeded the second call should throw a conflict
        // taskId unique constraint is violated
        resultSecondCall.then().assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/database-conflict"))
            .body("title", equalTo("Database Conflict Error"))
            .body("status", equalTo(503))
            .body("detail", equalTo(
                "Database Conflict Error: The action could not be completed because "
                + "there was a conflict in the database."));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_500_if_no_case_id() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        String taskId = UUID.randomUUID().toString();

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));
        //First call
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_case_id_is_invalid() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someInvalidCaseID"),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_task_id_does_not_exist() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            UUID.randomUUID().toString(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }
}

