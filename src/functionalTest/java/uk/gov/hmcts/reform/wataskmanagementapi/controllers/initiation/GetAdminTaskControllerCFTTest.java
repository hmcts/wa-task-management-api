package uk.gov.hmcts.reform.wataskmanagementapi.controllers.initiation;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootTasksMapTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

public class GetAdminTaskControllerCFTTest extends SpringBootTasksMapTest {
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

        initiateTaskMap(taskVariables, Jurisdiction.IA);

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
}
