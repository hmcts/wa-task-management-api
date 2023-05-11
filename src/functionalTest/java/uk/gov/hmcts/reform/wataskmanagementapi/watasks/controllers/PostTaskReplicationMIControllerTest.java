package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;


@SuppressWarnings("checkstyle:LineLength")
public class PostTaskReplicationMIControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED_TASK = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED_HISTORY = "/task/{task-id}/history";
    private static final String ENDPOINT_BEING_TESTED_REPORTABLE = "/task/{task-id}/reportable";

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
    public void user_should_configure_task_and_configure_action_recorded_in_replicate_db() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        String taskId = taskVariables.getTaskId();
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(2))
            .body("task_history_list.get(0).state", equalTo("UNASSIGNED"))
            .body("task_history_list.get(0).assignee", equalTo(null))
            .body("task_history_list.get(0).updated_by", notNullValue())
            .body("task_history_list.get(0).updated", notNullValue())
            .body("task_history_list.get(0).update_action", equalTo("Configure"))
            .body("task_history_list.get(1).state", equalTo("UNASSIGNED"))
            .body("task_history_list.get(1).assignee", equalTo(null))
            .body("task_history_list.get(1).updated_by", notNullValue())
            .body("task_history_list.get(1).updated", notNullValue())
            .body("task_history_list.get(1).update_action", equalTo("Configure"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_and_claim_action_recorded_in_replicate_db() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(3))
            .body("task_history_list.get(0).state", equalTo("UNASSIGNED"))
            .body("task_history_list.get(0).assignee", equalTo(null))
            .body("task_history_list.get(0).updated_by", notNullValue())
            .body("task_history_list.get(0).updated", notNullValue())
            .body("task_history_list.get(0).update_action", equalTo("Configure"))
            .body("task_history_list.get(1).state", equalTo("UNASSIGNED"))
            .body("task_history_list.get(1).assignee", equalTo(null))
            .body("task_history_list.get(1).updated_by", notNullValue())
            .body("task_history_list.get(1).updated", notNullValue())
            .body("task_history_list.get(1).update_action", equalTo("Configure"))
            .body("task_history_list.get(2).state", equalTo("ASSIGNED"))
            .body("task_history_list.get(2).assignee", notNullValue())
            .body("task_history_list.get(2).updated_by", notNullValue())
            .body("task_history_list.get(2).updated", notNullValue())
            .body("task_history_list.get(2).update_action", equalTo("Claim"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_delete_task_and_action_recorded_in_replicate_db() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response resultDelete = restApiActions.delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(4))
            .body("task_history_list.get(0).update_action", equalTo("Configure"))
            .body("task_history_list.get(1).update_action", equalTo("Configure"))
            .body("task_history_list.get(2).update_action", equalTo("Claim"))
            .body("task_history_list.get(3).state", equalTo("TERMINATED"))
            .body("task_history_list.get(2).assignee", notNullValue())
            .body("task_history_list.get(3).updated_by", notNullValue())
            .body("task_history_list.get(3).updated", notNullValue())
            .body("task_history_list.get(3).update_action", equalTo("AutoCancel"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_configure_task_and_configure_action_recorded_in_reportable_task() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        String taskId = taskVariables.getTaskId();
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("UNASSIGNED"))
            .body("reportable_task_list.get(0).assignee", equalTo(null))
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_and_claim_action_recorded_in_reportable_task() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();

        Response resultTaskReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultTaskReportable.prettyPrint();
        resultTaskReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).task_id", equalTo(taskId))
            .body("reportable_task_list.get(0).state", equalTo("UNASSIGNED"))
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"));

        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"));

        common.cleanUpTask(taskId);
    }

}
