package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.path.json.JsonPath;
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

import java.time.OffsetDateTime;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskReplicationMIControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED_TASK = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED_HISTORY = "/task/{task-id}/history";
    private static final String ENDPOINT_BEING_TESTED_REPORTABLE = "/task/{task-id}/reportable";
    private static final String ENDPOINT_BEING_TESTED_COMPLETE = "task/{task-id}/complete";
    private static final String ENDPOINT_BEING_TESTED_ASSIGNMENTS = "/task/{task-id}/assignments";
    private static final String ENDPOINT_BEING_TESTED_UNCLAIM = "task/{task-id}/unclaim";
    private static final String ENDPOINT_BEING_TESTED_CANCEL = "task/{task-id}/cancel";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials caseworkerCredentials2;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        caseworkerCredentials2 = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r3-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void user_should_configure_task_and_configure_action_recorded_in_replica_tables() {

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
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"))
            .body("reportable_task_list.get(0).first_assigned_date", nullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", nullValue())
            .body("reportable_task_list.get(0).wait_time_days", nullValue())
            .body("reportable_task_list.get(0).wait_time", nullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).completed_date", nullValue())
            .body("reportable_task_list.get(0).completed_date_time", nullValue())
            .body("reportable_task_list.get(0).final_state_label", nullValue())
            .body("reportable_task_list.get(0).handling_time_days", nullValue())
            .body("reportable_task_list.get(0).handling_time", nullValue())
            .body("reportable_task_list.get(0).processing_time_days", nullValue())
            .body("reportable_task_list.get(0).processing_time", nullValue())
            .body("reportable_task_list.get(0).is_within_sla", nullValue())
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", nullValue())
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", nullValue());

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list", empty());

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

        JsonPath configureJsonPathEvaluator = resultTaskReportable.jsonPath();

        assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultTaskReportable.prettyPrint();
        resultTaskReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).task_id", equalTo(taskId))
            .body("reportable_task_list.get(0).state", equalTo("UNASSIGNED"))
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"))
            .body("reportable_task_list.get(0).first_assigned_date", nullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", nullValue())
            .body("reportable_task_list.get(0).wait_time_days", nullValue())
            .body("reportable_task_list.get(0).wait_time", nullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0));

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

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();

        assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isEqual(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))));
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertNotEquals(configureJsonPathEvaluator.get("reportable_task_list.get(0).updated_by").toString(),
                        claimJsonPathEvaluator.get("reportable_task_list.get(0).updated_by").toString());

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_configure_claim_unclaim_and_reclaim_task_actions_recorded_in_replica_tables() {

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

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).handling_time", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", equalTo(null));

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1));

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "task-supervisor");
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED_UNCLAIM,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultAssignmentsUnclaim = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"));

        resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(4));

        resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("UNASSIGNED"))
            .body("reportable_task_list.get(0).assignee", nullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Unclaim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0));

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response resultAssignmentsClaim = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", nullValue());

        resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(5));

        Response resultReport = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath reClaimJsonPathEvaluator = resultReport.jsonPath();

        assertTrue(OffsetDateTime.parse(reClaimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(reClaimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(reClaimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                     reClaimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                     reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                     reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                     reClaimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                     reClaimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString());

        resultReport.prettyPrint();
        resultReport.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(1))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_and_delete_task_action_recorded_in_replicate_db() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
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

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).handling_time", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", equalTo(null));

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
            .body("task_history_list.get(3).assignee", notNullValue())
            .body("task_history_list.get(3).updated_by", notNullValue())
            .body("task_history_list.get(3).updated", notNullValue())
            .body("task_history_list.get(3).update_action", equalTo("AutoCancel"));

        Response resultDeleteReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath deleteJsonPathEvaluator = resultDeleteReportable.jsonPath();

        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                     deleteJsonPathEvaluator.get("reportable_task_list.get(0).created").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                     deleteJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                     deleteJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                     deleteJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                     deleteJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString());
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isEqual(OffsetDateTime.parse(deleteJsonPathEvaluator.get("reportable_task_list.get(0).created"))));
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(deleteJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultDeleteReportable.prettyPrint();
        resultDeleteReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("AutoCancel"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("AUTO_CANCELLED"))
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).handling_time", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(null))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", equalTo(null));

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).service", equalTo("WA"))
            .body("task_assignments_list.get(0).location", equalTo("765324"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", notNullValue())
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("CANCELLED"))
            .body("task_assignments_list.get(0).assignee", notNullValue())
            .body("task_assignments_list.get(0).role_category", equalTo("LEGAL_OPERATIONS"));

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_claim_complete_task_and_complete_action_recorded_in_replica_tables() {

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

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", nullValue())
            .body("reportable_task_list.get(0).processing_time_days", nullValue())
            .body("reportable_task_list.get(0).is_within_sla", nullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", nullValue());

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).service", equalTo("WA"))
            .body("task_assignments_list.get(0).location", equalTo("765324"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", equalTo(null))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo(null))
            .body("task_assignments_list.get(0).assignee", notNullValue())
            .body("task_assignments_list.get(0).role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task_assignments_list.get(0).task_name", equalTo("Process Application"));

        Response resultComplete = restApiActions.post(
            ENDPOINT_BEING_TESTED_COMPLETE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(4));

        Response resultCompleteReport = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath completeJsonPathEvaluator = resultCompleteReport.jsonPath();

        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).created").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString());
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultCompleteReport.prettyPrint();
        resultCompleteReport.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("COMPLETED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Complete"))
            .body("reportable_task_list.get(0).completed_date", notNullValue())
            .body("reportable_task_list.get(0).completed_date_time", notNullValue())
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", notNullValue())
            .body("reportable_task_list.get(0).handling_time", notNullValue())
            .body("reportable_task_list.get(0).processing_time_days", notNullValue())
            .body("reportable_task_list.get(0).processing_time", notNullValue())
            .body("reportable_task_list.get(0).is_within_sla", equalTo("Yes"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(-10))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", notNullValue());

        resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).service", equalTo("WA"))
            .body("task_assignments_list.get(0).location", equalTo("765324"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", notNullValue())
            .body("task_assignments_list.get(0).assignee", notNullValue())
            .body("task_assignments_list.get(0).role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task_assignments_list.get(0).task_name", equalTo("Process Application"))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("COMPLETED"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_complete_terminate_task_and_actions_recorded_in_replica_tables() {

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

        Response resultReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", nullValue())
            .body("reportable_task_list.get(0).processing_time_days", nullValue())
            .body("reportable_task_list.get(0).is_within_sla", nullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", nullValue());

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).service", equalTo("WA"))
            .body("task_assignments_list.get(0).location", equalTo("765324"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", equalTo(null))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo(null))
            .body("task_assignments_list.get(0).assignee", notNullValue())
            .body("task_assignments_list.get(0).role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task_assignments_list.get(0).task_name", equalTo("Process Application"));

        Response resultComplete = restApiActions.post(
            ENDPOINT_BEING_TESTED_COMPLETE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(4));

        Response resultCompleteReport = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath completeJsonPathEvaluator = resultCompleteReport.jsonPath();

        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).created").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                     completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString());
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultCompleteReport.prettyPrint();
        resultCompleteReport.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("COMPLETED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Complete"))
            .body("reportable_task_list.get(0).completed_date", notNullValue())
            .body("reportable_task_list.get(0).completed_date_time", notNullValue())
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", notNullValue())
            .body("reportable_task_list.get(0).handling_time", notNullValue())
            .body("reportable_task_list.get(0).processing_time_days", notNullValue())
            .body("reportable_task_list.get(0).processing_time", notNullValue())
            .body("reportable_task_list.get(0).is_within_sla", equalTo("Yes"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(-10))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", notNullValue());

        resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).service", equalTo("WA"))
            .body("task_assignments_list.get(0).location", equalTo("765324"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", notNullValue())
            .body("task_assignments_list.get(0).assignee", notNullValue())
            .body("task_assignments_list.get(0).role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task_assignments_list.get(0).task_name", equalTo("Process Application"))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("COMPLETED"));


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

        resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(5))
            .body("task_history_list.get(0).update_action", equalTo("Configure"))
            .body("task_history_list.get(1).update_action", equalTo("Configure"))
            .body("task_history_list.get(2).update_action", equalTo("Claim"))
            .body("task_history_list.get(3).update_action", equalTo("Complete"))
            .body("task_history_list.get(4).update_action", equalTo("AutoCancel"))
            .body("task_history_list.get(4).state", equalTo("TERMINATED"))
            .body("task_history_list.get(4).assignee", notNullValue())
            .body("task_history_list.get(4).updated_by", notNullValue())
            .body("task_history_list.get(4).updated", notNullValue());

        Response resultTerminateReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        JsonPath terminateJsonPathEvaluator = resultTerminateReportable.jsonPath();

        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).created").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString());
        assertEquals(claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).completed_date_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).completed_date_time").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).completed_date").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).completed_date").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).handling_time_days").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).handling_time_days").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).handling_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).handling_time").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).processing_time_days").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).processing_time_days").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).processing_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).processing_time").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).is_within_sla").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).is_within_sla").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).number_of_reassignments").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).number_of_reassignments").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).due_date_to_completed_diff_days").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).due_date_to_completed_diff_days").toString());
        assertEquals(completeJsonPathEvaluator.get("reportable_task_list.get(0).due_date_to_completed_diff_time").toString(),
                     terminateJsonPathEvaluator.get("reportable_task_list.get(0).due_date_to_completed_diff_time").toString());
        assertTrue(OffsetDateTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(terminateJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

        resultTerminateReportable.prettyPrint();
        resultTerminateReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("AutoCancel"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("AUTO_CANCELLED"))
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).completed_date", notNullValue())
            .body("reportable_task_list.get(0).completed_date_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", notNullValue())
            .body("reportable_task_list.get(0).handling_time", notNullValue())
            .body("reportable_task_list.get(0).processing_time_days", notNullValue())
            .body("reportable_task_list.get(0).processing_time", notNullValue())
            .body("reportable_task_list.get(0).is_within_sla", equalTo("Yes"))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", notNullValue())
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", notNullValue());


        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_cancel_task_when_role_assignment_verification_passed() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestJudiciary",
            "Review Specific Access Request Judiciary");

        common.setupLeadJudgeForSpecificAccess(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION);
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials2.getHeaders(), "judge");

        initiateTask(taskVariables, caseworkerCredentials2.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED_CANCEL,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultCancelReport = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultCancelReport.prettyPrint();
        resultCancelReport.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("CANCELLED"))
            .body("reportable_task_list.get(0).assignee", equalTo(null))
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Cancel"))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).completed_date", nullValue())
            .body("reportable_task_list.get(0).completed_date_time", nullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("USER_CANCELLED"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).wait_time_days", nullValue())
            .body("reportable_task_list.get(0).wait_time", nullValue())
            .body("reportable_task_list.get(0).first_assigned_date", nullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", nullValue());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_configure_claim_unclaim_multiple_times_for_reassignments_check() {

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

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "task-supervisor");
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED_UNCLAIM,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultAssignmentsUnclaim = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"));

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        resultAssignmentsUnclaim = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"));

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            caseworkerCredentials.getHeaders(),
            HttpStatus.NO_CONTENT
        );


        Response resultAssignmentsClaim = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", nullValue());

        Response resultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(6));

        Response resultReport = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultReport.prettyPrint();
        resultReport.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(2))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue());

        common.cleanUpTask(taskId);
    }

}
