package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;

public class GetAdminTaskControllerCFTTest extends SpringBootFunctionalBaseTest {
    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getAdminUserAuthorization("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_200_with_allocateHearingJudge_task_and_standard_grant_type() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds("allocateHearingJudge");
        String taskId = taskVariables.getTaskId();

        initiateTaskForAdmin(taskVariables);

        Headers headers = caseworkerCredentials.getHeaders();
        common.setupCFTAdministrativeOrganisationalRoleAssignment(headers);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            headers
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
            .body("task.permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("task.permissions.values", hasSize(5))
            .body("task.description", notNullValue())
            .body("task.role_category", equalTo("ADMIN"));

        common.cleanUpTask(taskId);
    }

    private void initiateTaskForAdmin(TestVariables taskVariables) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "allocateHearingJudge",
            TASK_NAME.value(), "Allocate hearing judge",
            TITLE.value(), "A test task",
            CASE_ID.value(), taskVariables.getCaseId(),
            CREATED.value(), formattedCreatedDate,
            DUE_DATE.value(), formattedDueDate,
            ROLE_CATEGORY.value(), "ADMIN"
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
}
