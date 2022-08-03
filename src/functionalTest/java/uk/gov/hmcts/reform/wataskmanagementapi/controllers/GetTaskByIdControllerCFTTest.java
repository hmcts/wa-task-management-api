package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.*;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class GetTaskByIdControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");

    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);
        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
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
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_location_match_organisational_role() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);
        Response result;

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(false))
            .body("task.case_management_category", equalTo("Protection"))
            .body("task.work_type_id", equalTo("decision_making_work"))
            .body("task.permissions.values.size()", equalTo(5))
            .body("task.permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id_jurisdiction_location_and_region_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);
        Response result;
        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "1"
            )
        );

        result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(false));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        assertTrue(actualWarnings.isEmpty());
        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_200_with_task_warnings() {

        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        initiateTaskWithWarnings(taskVariables, "reviewTheAppeal");

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(true))
            .body("task.permissions.values.size()", equalTo(5))
            .body("task.permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "Code1", "warningText", "Text1"),
            Map.of("warningCode", "Code2", "warningText", "Text2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_task_and_correct_properties() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        initiateTaskWithWarnings(taskVariables, "reviewTheAppeal");

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "1"
            )
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("task.id", notNullValue())
            .body("task.name", notNullValue())
            .body("task.type", notNullValue())
            .body("task.task_state", notNullValue())
            .body("task.task_system", notNullValue())
            .body("task.security_classification", notNullValue())
            .body("task.task_title", notNullValue())
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.location_name", notNullValue())
            .body("task.location", notNullValue())
            .body("task.execution_type", notNullValue())
            .body("task.jurisdiction", notNullValue())
            .body("task.region", notNullValue())
            .body("task.case_type_id", notNullValue())
            .body("task.case_id", notNullValue())
            .body("task.work_type_id", notNullValue())
            .body("task.work_type_label", notNullValue())
            .body("task.case_category", notNullValue())
            .body("task.case_name", notNullValue())
            .body("task.auto_assigned", notNullValue())
            .body("task.warnings", notNullValue())
            .body("task.permissions.values.size()", equalTo(5))
            .body("task.permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("task.description", notNullValue())
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_403_when_the_user_did_not_have_sufficient_permission_region_did_not_match() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(REGION, "1");
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA",
                "region", "2"
            )
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_retrieve_a_task_by_id_jurisdiction_location_match_org_role_when_r2_endpoint_flag_is_on() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        initiateTask(taskVariables);


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(false));


        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_work_type() {

        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        initiateTaskWithWarnings(taskVariables, "reviewTheAppeal");

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.work_type_id", equalTo("decision_making_work"))
            .body("task.work_type_label", equalTo("Decision-making work"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_task_description_property() {
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds("reviewTheAppeal");
        String taskId = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");

        initiateTaskWithWarnings(taskVariables1, "reviewTheAppeal");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("task.id", notNullValue())
            .body("task.name", notNullValue())
            .body("task.type", notNullValue())
            .body("task.task_state", notNullValue())
            .body("task.task_system", notNullValue())
            .body("task.security_classification", notNullValue())
            .body("task.task_title", notNullValue())
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.location_name", notNullValue())
            .body("task.location", notNullValue())
            .body("task.execution_type", notNullValue())
            .body("task.jurisdiction", notNullValue())
            .body("task.region", notNullValue())
            .body("task.case_type_id", notNullValue())
            .body("task.case_id", notNullValue())
            .body("task.case_type_id", notNullValue())
            .body("task.case_category", notNullValue())
            .body("task.case_name", notNullValue())
            .body("task.auto_assigned", notNullValue())
            .body("task.warnings", notNullValue())
            .body("task.description", notNullValue());

        common.cleanUpTask(taskId);
    }

    private void initiateTask(TestVariables taskVariables) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
            TITLE.value(), "A test task",
            CASE_ID.value(), taskVariables.getCaseId(),
            CREATED.value(), formattedCreatedDate,
            DUE_DATE.value(), formattedDueDate
        );

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, taskAttributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }

    private void initiateTaskWithWarnings(TestVariables taskVariables, String taskType) {
        WarningValues warningValues = new WarningValues(
            asList(
                new Warning("Code1", "Text1"),
                new Warning("Code2", "Text2")
            ));

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(TASK_TYPE.value(), taskType);
        attributes.put(TASK_NAME.value(), "follow Up Overdue Reasons For Appeal");
        attributes.put(TITLE.value(), "A test task");
        attributes.put(TASK_SYSTEM.value(), "SELF");
        attributes.put(CASE_ID.value(), taskVariables.getCaseId());
        attributes.put(CREATED.value(), formattedCreatedDate);
        attributes.put(DUE_DATE.value(), formattedDueDate);
        attributes.put(SECURITY_CLASSIFICATION.value(), SecurityClassification.PUBLIC);
        attributes.put(ROLE_CATEGORY.value(), "ADMIN");
        attributes.put(LOCATION_NAME.value(), "aLocationName");
        attributes.put(LOCATION.value(), "aLocation");
        attributes.put(EXECUTION_TYPE.value(), "Manual");
        attributes.put(JURISDICTION.value(), "IA");
        attributes.put(REGION_NAME.value(), "aRegion");
        attributes.put(CASE_TYPE_ID.value(), "aTaskCaseTypeId");
        attributes.put(CASE_CATEGORY.value(), "Protection");
        attributes.put(CASE_NAME.value(), "aCaseName");
        attributes.put(AUTO_ASSIGNED.value(), true);
        attributes.put(HAS_WARNINGS.value(), true);
        attributes.put(WARNING_LIST.value(), warningValues);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, attributes);

        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

    }
}
