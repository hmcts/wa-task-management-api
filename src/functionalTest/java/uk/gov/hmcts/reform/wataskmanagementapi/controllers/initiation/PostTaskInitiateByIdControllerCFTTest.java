package uk.gov.hmcts.reform.wataskmanagementapi.controllers.initiation;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootTasksMapTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestAttributes;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.*;

public class PostTaskInitiateByIdControllerCFTTest extends SpringBootTasksMapTest {

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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("reviewHearingBundle", "review Hearing Bundle");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("review Hearing Bundle"))
                .body("task.type", equalTo("reviewHearingBundle"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("review Hearing Bundle"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("Asylum"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("IA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.permissions.values.size()", equalTo(5))
                .body("task.permissions.values", hasItems("Read", "Refer", "Manage", "Execute", "Cancel"));
        };

        initiateTaskMap(taskVariables, Jurisdiction.IA, assertConsumer);

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_hearing_centre_admin_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("allocateHearingJudge", "allocate Hearing Judge");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("allocate Hearing Judge"))
                .body("task.type", equalTo("allocateHearingJudge"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("allocate Hearing Judge"))
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("Asylum"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("IA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.permissions.values.size()", equalTo(5))
                .body("task.permissions.values", hasItems("Read", "Refer", "Execute", "Manage", "Cancel"));
        };

        initiateTaskMap(taskVariables, Jurisdiction.IA, assertConsumer);

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_national_business_centre_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("arrangeOfflinePayment",
                                                                     "arrange Offline Payment");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("arrange Offline Payment"))
                .body("task.type", equalTo("arrangeOfflinePayment"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("arrange Offline Payment"))
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("Asylum"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("IA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("routine_work"))
                .body("task.work_type_label", equalTo("Routine work"))
                .body("task.permissions.values.size()", equalTo(5))
                .body("task.permissions.values", hasItems("Read", "Refer", "Execute", "Manage", "Cancel"));
        };

        initiateTaskMap(taskVariables, Jurisdiction.IA, assertConsumer);

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_default_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("aTaskType", "aTaskName");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("aTaskName"))
                .body("task.type", equalTo("aTaskType"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("aTaskName"))
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("Asylum"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("IA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.permissions.values.size()", equalTo(5))
                .body("task.permissions.values", hasItems("Read", "Refer", "Execute", "Manage", "Cancel"));
        };

        initiateTaskMap(taskVariables, Jurisdiction.IA, assertConsumer);

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

        Consumer<Response> assertConsumer = (result) -> {
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value());
        };

        initiateTaskMap(taskVariables, Jurisdiction.IA, assertConsumer);

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestAttributes req = new InitiateTaskRequestAttributes(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

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
}

