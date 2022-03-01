package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class GetAdminTaskControllerCFTTest extends SpringBootFunctionalBaseTest {
    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationProvider.getAdminUserAuthorization("wa-ft-test-r2-");

    }

    @Test
    public void should_return_a_200_with_allocateHearingJudge_task_and_standard_grant_type() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds("allocateHearingJudge");
        String taskId = taskVariables.getTaskId();

        initiateTaskForAdmin(taskVariables);

        common.setupCFTAdministrativeOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
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
            .body("task.case_category", equalTo("Protection"))
            .body("task.case_name", notNullValue())
            .body("task.auto_assigned", notNullValue())
            .body("task.warnings", notNullValue())
            .body("task.permissions.values", hasItems("Read", "Refer", "Own"))
            .body("task.permissions.values", hasSize(3))
            .body("task.description", notNullValue())
            .body("task.role_category", equalTo("ADMIN"));

        common.cleanUpTask(taskId);
    }

    private void initiateTaskForAdmin(TestVariables taskVariables) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "allocateHearingJudge"),
            new TaskAttribute(TASK_NAME, "Allocate hearing judge"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate),
            new TaskAttribute(TASK_ROLE_CATEGORY, "ADMIN")

        ));
        Response result = restApiActions.post(
            TASK_INITIATION_ENDPOINT_BEING_TESTED,
            taskVariables.getTaskId(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }
}
