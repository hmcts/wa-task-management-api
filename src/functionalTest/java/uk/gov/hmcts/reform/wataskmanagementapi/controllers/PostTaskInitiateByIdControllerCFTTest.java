package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;

public class PostTaskInitiateByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_201_when_initiating_a_judge_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "reviewHearingBundle",
                TASK_NAME.value(), "review Hearing Bundle",
                TITLE.value(), "review Hearing Bundle",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        //Note: this is the TaskResource.class
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("review Hearing Bundle"))
            .body("task_type", equalTo("reviewHearingBundle"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("review Hearing Bundle"))
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
            .body("work_type_resource.id", equalTo("hearing_work"))
            .body("work_type_resource.label", equalTo("Hearing work"))
            .body("task_role_resources.size()", equalTo(5));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", true),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );
        assertPermissions(
            getTaskResource(result, "hearing-judge"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", false),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "JUDICIAL"),
                entry("auto_assignable", true),
                entry("assignment_priority", 1)
            )
        );
        assertPermissions(
            getTaskResource(result, "judge"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of("373")),
                entry("role_category", "JUDICIAL"),
                entry("auto_assignable", false),
                entry("assignment_priority", 1)
            )
        );

        assertPermissions(
            getTaskResource(result, "senior-tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", true),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );
        assertPermissions(
            getTaskResource(result, "tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", true),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_hearing_centre_admin_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "allocateHearingJudge",
                TASK_NAME.value(), "allocate Hearing Judge",
                TITLE.value(), "allocate Hearing Judge",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("allocate Hearing Judge"))
            .body("task_type", equalTo("allocateHearingJudge"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("allocate Hearing Judge"))
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
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
            .body("work_type_resource.id", equalTo("hearing_work"))
            .body("work_type_resource.label", equalTo("Hearing work"))
            .body("task_role_resources.size()", equalTo(2));

        assertPermissions(
            getTaskResource(result, "hearing-centre-admin"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "ADMIN"),
                entry("auto_assignable", false),
                entry("assignment_priority", 1)
            )
        );

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", true),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );
        assertPermissions(
            getTaskResource(result, "hearing-centre-admin"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "ADMIN"),
                entry("auto_assignable", false),
                entry("assignment_priority", 1)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_national_business_centre_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "arrangeOfflinePayment",
                TASK_NAME.value(), "arrange Offline Payment",
                TITLE.value(), "arrange Offline Payment",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("arrange Offline Payment"))
            .body("task_type", equalTo("arrangeOfflinePayment"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("arrange Offline Payment"))
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
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
            .body("work_type_resource.id", equalTo("routine_work"))
            .body("work_type_resource.label", equalTo("Routine work"))
            .body("task_role_resources.size()", equalTo(1));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", true),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_default_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "aTaskType",
                TASK_NAME.value(), "aTaskName",
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
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
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("Asylum"))
            .body("case_name", equalTo("Bob Smith"))
            .body("case_category", equalTo("Protection"))
            .body("jurisdiction", equalTo("IA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body(
                "execution_type_code.description",
                equalTo(
                    "The task requires a case management event to be executed by the user."
                        + " (Typically this will be in CCD.)")
            )
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body("task_role_resources.size()", equalTo(1));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.of("read", true,
                   "refer", true,
                   "own", false,
                   "manage", true,
                   "execute", true,
                   "cancel", true,
                   "task_id", taskId,
                   "authorizations", List.of(),
                   "auto_assignable", false
            )
        );

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
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                HAS_WARNINGS.value(), true,
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        //First call
        Response resultFirstCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        resultFirstCall.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        //Second call
        Response resultSecondCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
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
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                HAS_WARNINGS.value(), true,
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId()
            )
        );
        //First call
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(400))
            .body("violations[0].field", equalTo("dueDate"))
            .body("violations[0].message",
                equalTo("Each task to initiate must contain dueDate field present and populated."));
    }

    @Test
    public void should_return_a_500_if_no_case_id() {
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        String taskId = UUID.randomUUID().toString();


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                DUE_DATE.value(), formattedDueDate
            )
        );

        //First call
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_case_id_is_invalid() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                CASE_ID.value(), "someInvalidCaseID",
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                DUE_DATE.value(), formattedDueDate
            )
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_task_id_does_not_exist() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                CASE_ID.value(), taskVariables.getCaseId(),
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                DUE_DATE.value(), formattedDueDate
            )
        );
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            UUID.randomUUID().toString(),
            req,
            caseworkerCredentials.getHeaders()
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

