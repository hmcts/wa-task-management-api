package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class PostTaskInitiateByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-");
    }

    @Test
    public void should_return_a_201_when_initiating_a_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        //Note: this is the TaskResource.class
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("follow Up Overdue Reasons For Appeal"))
            .body("task_type", equalTo("followUpOverdueReasonsForAppeal"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("follow Up Overdue Reasons For Appeal"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
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
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                        + "(Typically this will be in CCD.)")
            )
            .body("task_role_resources[0].task_role_id", notNullValue())
            .body("task_role_resources[0].task_id", equalTo(taskId))
            .body(
                "task_role_resources[0].role_name",
                anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))
            )
            .body("task_role_resources[0].read", equalTo(true))
            .body("task_role_resources[0].own", equalTo(true))
            .body("task_role_resources[0].execute", equalTo(false))
            .body("task_role_resources[0].cancel", equalTo(true))
            .body("task_role_resources[0].refer", equalTo(true))
            .body("task_role_resources[0].authorizations", equalTo(emptyList()))
            .body("task_role_resources[0].auto_assignable", equalTo(false))
            .body(
                "task_role_resources[0].role_category",
                anyOf(is("LEGAL_OPERATIONS"), is(nullValue()))
            )
            .body("task_role_resources[1].task_role_id", notNullValue())
            .body("task_role_resources[1].task_id", equalTo(taskId))
            .body(
                "task_role_resources[1].role_name",
                anyOf(is("tribunal-caseworker"), is("senior-tribunal-caseworker"))
            )
            .body("task_role_resources[1].read", equalTo(true))
            .body("task_role_resources[1].own", equalTo(true))
            .body("task_role_resources[1].execute", equalTo(false))
            .body("task_role_resources[1].cancel", equalTo(true))
            .body("task_role_resources[1].refer", equalTo(true))
            .body("task_role_resources[1].authorizations", equalTo(emptyList()))
            .body("task_role_resources[1].auto_assignable", equalTo(false))
            .body("task_role_resources[1].role_category",
                anyOf(is("LEGAL_OPERATIONS"), is(nullValue())))
            .body("work_type_resource.id", equalTo("decision_making_work"))
            .body("work_type_resource.label", equalTo("Decision-making work"));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }


    @Test
    public void should_return_a_201_when_initiating_a_task_by_id_with_no_created_date() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.prettyPrint();
        //Note: this is the TaskResource.class
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("follow Up Overdue Reasons For Appeal"))
            .body("task_type", equalTo("followUpOverdueReasonsForAppeal"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("follow Up Overdue Reasons For Appeal"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
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
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                        + "(Typically this will be in CCD.)")
            )
            .body("work_type_resource.id", equalTo("decision_making_work"))
            .body("work_type_resource.label", equalTo("Decision-making work"));

        assertPermissions(
            getTaskResource(result, "tribunal-caseworker"),
            Map.of("read", true,
                "refer", true,
                "own", true,
                "manage", true,
                "execute", false,
                "cancel", true,
                "task_id", taskId,
                "authorizations", List.of(),
                "auto_assignable", false
            )
        );

        assertPermissions(
            getTaskResource(result, "senior-tribunal-caseworker"),
            Map.of("read", true,
                "refer", true,
                "own", true,
                "manage", true,
                "execute", false,
                "cancel", true,
                "task_id", taskId,
                "authorizations", List.of(),
                "auto_assignable", false
            )
        );

        Optional.ofNullable(getTaskResource(result, "task-supervisor"))
            .ifPresent(resource ->
                assertPermissions(
                    resource,
                    Map.of("read", true,
                        "refer", true,
                        "own", false,
                        "manage", true,
                        "execute", false,
                        "cancel", true,
                        "task_id", taskId,
                        "authorizations", List.of("IA"),
                        "auto_assignable", false
                    )
                ));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_503_if_task_already_initiated() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
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
    public void should_return_a_400_if_no_due_date() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_CASE_ID, taskId)
        ));
        //First call
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(400))
            .body("violations[0].field", equalTo("task_due_date"))
            .body("violations[0].message",
                equalTo("Each task to initiate must contain task_due_date field present and populated."));
    }

    @Test
    public void should_return_a_500_if_no_case_id() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        String taskId = UUID.randomUUID().toString();


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
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

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, "someInvalidCaseID"),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
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

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
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

    private void assertPermissions(Map<String, Object> resource, Map<String, Object> expectedPermissions) {
        expectedPermissions.keySet().forEach(key ->
            assertThat(resource).containsEntry(key, expectedPermissions.get(key)));

        assertThat(resource.get("task_role_id")).isNotNull();
    }

    private Map<String, Object> getTaskResource(Response result, String roleName) {
        final List<Map<String, Object>> resources = new JsonPath(result.getBody().asString())
            .param("roleName", roleName)
            .get("task_role_resources.findAll { resource -> resource.role_name == roleName }");
        return resources.size() > 0 ? resources.get(0) : null;
    }
}

