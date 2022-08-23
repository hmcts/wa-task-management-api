package uk.gov.hmcts.reform.wataskmanagementapi.controllers.newinitiate;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class GetTaskByIdRolePermissionsCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/roles";
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
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(
            taskVariables,
            "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders()
        );

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
    public void should_return_a_404_if_task_does_not_exist() {
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

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
    public void should_return_200_and_retrieve_role_permission_information_for_given_task_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(
            taskVariables,
            "followUpOverdueReasonsForAppeal",
            caseworkerCredentials.getHeaders()
        );

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("roles.size()", equalTo(4))
            .body("roles[0].role_category", is("LEGAL_OPERATIONS"))
            .body("roles[0].role_name", is("case-manager"))
            .body("roles[0].permissions.size()",  equalTo(4))
            .body("roles[0].permissions", hasItems("Read", "Own", "Refer"))
            .body("roles[0].authorisations", empty())
            .body("roles[1].role_category", is("LEGAL_OPERATIONS"))
            .body("roles[1].role_name", is("senior-tribunal-caseworker"))
            .body("roles[1].permissions.size()",  equalTo(5))
            .body("roles[1].permissions", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("roles[1].authorisations", empty())
            .body("roles[2].role_name", is("task-supervisor"))
            .body("roles[2].permissions.size()",  equalTo(5))
            .body("roles[2].permissions", hasItems("Read", "Refer", "Manage", "Execute", "Cancel"))
            .body("roles[2].authorisations", empty())
            .body("roles[3].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[3].role_name", is("tribunal-caseworker"))
            .body("roles[3].permissions.size()",  equalTo(5))
            .body("roles[3].permissions", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("roles[3].authorisations", empty());

        common.cleanUpTask(taskId);
    }

}
