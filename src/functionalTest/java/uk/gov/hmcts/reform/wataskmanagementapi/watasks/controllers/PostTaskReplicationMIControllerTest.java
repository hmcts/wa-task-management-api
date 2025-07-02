package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("checkstyle:LineLength")
@SpringBootTest
public class PostTaskReplicationMIControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED_TASK = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED_HISTORY = "/task/{task-id}/history";
    private static final String ENDPOINT_BEING_TESTED_REPORTABLE = "/task/{task-id}/reportable";
    private static final String ENDPOINT_BEING_TESTED_COMPLETE = "task/{task-id}/complete";
    private static final String ENDPOINT_BEING_TESTED_ASSIGNMENTS = "/task/{task-id}/assignments";
    private static final String ENDPOINT_BEING_TESTED_UNCLAIM = "task/{task-id}/unclaim";
    private static final String ENDPOINT_BEING_TESTED_CANCEL = "task/{task-id}/cancel";
    public static final String LOCAL_ARM_ARCH = "local-arm-arch";

    @Value("${environment}")
    private String environment;

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
            .body("reportable_task_list.get(0).created", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"))
            .body("reportable_task_list.get(0).first_assigned_date", nullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", nullValue())
            .body("reportable_task_list.get(0).wait_time_days", nullValue())
            .body("reportable_task_list.get(0).wait_time", nullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0));

        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() ->  assertNotNull(taskId));

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

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0));

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isEqual(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))));
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertNotEquals(configureJsonPathEvaluator.get("reportable_task_list.get(0).updated_by").toString(),
                        claimJsonPathEvaluator.get("reportable_task_list.get(0).updated_by").toString());
        assertTrue(LocalTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                                   DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay() > 1);

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_configure_claim_unclaim_and_reclaim_task_actions_recorded_in_replica_tables() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() ->  assertNotNull(taskId));

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

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
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

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertTrue(LocalTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                                   DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay() > 1);

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.prettyPrint();
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1));

        JsonPath resAssignmentsJsonPathEvaluator = resultAssignments.jsonPath();
        JsonPath resHistoryJsonPathEvaluator = resultHistory.jsonPath();

        assertEquals(resHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     resAssignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());

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
        resultAssignmentsUnclaim.prettyPrint();
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", notNullValue());

        Response resultUnclaimHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultUnclaimHistory.prettyPrint();
        resultUnclaimHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(4));

        JsonPath resUnClaimAssignmentsJsonPathEvaluator = resultAssignmentsUnclaim.jsonPath();
        JsonPath resUnClaimHistoryJsonPathEvaluator = resultUnclaimHistory.jsonPath();

        assertEquals(resUnClaimHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     resUnClaimAssignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());
        assertEquals(resUnClaimHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                     resUnClaimAssignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString());

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
        resultAssignmentsClaim.prettyPrint();
        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", nullValue())
            .body("task_assignments_list.get(0).assignment_start", notNullValue())
            .body("task_assignments_list.get(0).assignment_end", notNullValue())
            .body("task_assignments_list.get(1).assignment_start", notNullValue())
            .body("task_assignments_list.get(1).assignment_end", nullValue());

        Response reClaimResultHistory = restApiActions.get(
            ENDPOINT_BEING_TESTED_HISTORY,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        reClaimResultHistory.prettyPrint();
        reClaimResultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_history_list.size()", equalTo(5));

        JsonPath assignmentsJsonPathEvaluator = resultAssignmentsClaim.jsonPath();
        JsonPath resultHistoryJsonPathEvaluator = reClaimResultHistory.jsonPath();

        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());
        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString());
        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(4).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(1).assignment_start").toString());

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
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(1))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
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
        assertTrue(LocalTime.parse(reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                                   DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay() > 1);

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

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
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

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

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
            .body("task_history_list.get(3).update_action", equalTo("AutoCancel"))
            .body("task_history_list.get(3).termination_reason", equalTo("cancelled"));

        Response resultDeleteReportable = restApiActions.get(
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultDeleteReportable.prettyPrint();
        resultDeleteReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("AutoCancel"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo("AUTO_CANCELLED"))
            .body("reportable_task_list.get(0).termination_reason", equalTo("cancelled"))
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

        Response resultAssignments = restApiActions.get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseworkerCredentials.getHeaders()
        );
        resultAssignments.prettyPrint();
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

        JsonPath assignmentsJsonPathEvaluator = resultAssignments.jsonPath();
        JsonPath resultHistoryJsonPathEvaluator = resultHistory.jsonPath();

        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());
        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString());

        common.cleanUpTask(taskId);
    }


    @Test
    public void user_should_claim_complete_task_and_complete_action_recorded_in_replica_tables() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
            "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() ->  assertNotNull(taskId));

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
            .body("reportable_task_list.get(0).created", notNullValue())
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

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));
        assertTrue(LocalTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                                   DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay() > 1);

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

        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() ->  assertNotNull(taskId));

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

        int waitTimeSeconds = LocalTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                                              DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay();
        int processingTimeSeconds = LocalTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).processing_time").toString(),
                                                    DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay();
        int handlingTimeSeconds = LocalTime.parse(completeJsonPathEvaluator.get("reportable_task_list.get(0).handling_time").toString(),
                                                  DateTimeFormatter.ofPattern("HH:mm:ss")).toSecondOfDay();
        assertTrue(waitTimeSeconds > 1);
        assertTrue(processingTimeSeconds > 1);
        assertEquals(handlingTimeSeconds, processingTimeSeconds - waitTimeSeconds);

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

        JsonPath assignmentsJsonPathEvaluator = resultAssignments.jsonPath();
        JsonPath resultHistoryJsonPathEvaluator = resultHistory.jsonPath();

        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());
        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString());

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

        resultReportable.prettyPrint();
        resultReportable.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).created", notNullValue())
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

        JsonPath claimJsonPathEvaluator = resultReportable.jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))));

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

        JsonPath assignmentsJsonPathEvaluator = resultAssignments.jsonPath();
        JsonPath resultHistoryJsonPathEvaluator = resultHistory.jsonPath();

        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString());
        assertEquals(resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                     assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString());

        common.cleanUpTask(taskId);
    }

    @Test
    public void user_should_complete_task_and_termination_process_recorded_in_replica_tables() {
        TestAuthenticationCredentials userWithCompletionProcessEnabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-enabled-");

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(userWithCompletionProcessEnabled.getHeaders(), "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            userWithCompletionProcessEnabled.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultReportable = restApiActions.get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    userWithCompletionProcessEnabled.getHeaders()
                );


                resultReportable.prettyPrint();
                resultReportable.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1));
            });


        Response resultComplete = restApiActions.post(
            ENDPOINT_BEING_TESTED_COMPLETE + "?completion_process=" + "EXUI_CASE-EVENT_COMPLETION",
            taskId,
            userWithCompletionProcessEnabled.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        if (LOCAL_ARM_ARCH.equals(environment)) {
            TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
                new TerminateInfo("completed")
            );
            Response resultTerminate = restApiActions.delete(
                ENDPOINT_BEING_TESTED_TASK,
                taskId,
                terminateTaskRequest,
                userWithCompletionProcessEnabled.getHeaders()
            );
            resultTerminate.then().assertThat()
                .statusCode(HttpStatus.NO_CONTENT.value());
        }

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            userWithCompletionProcessEnabled.getHeaders()
        );
        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task.termination_process", equalTo("EXUI_CASE-EVENT_COMPLETION"))
            .body("task.id", equalTo(taskId));

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = restApiActions.get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    userWithCompletionProcessEnabled.getHeaders()
                );

                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(5))
                    .body("task_history_list.get(3).termination_process", equalTo("EXUI_CASE_EVENT_COMPLETION"))
                    .body("task_history_list.get(3).update_action", equalTo("Complete"))
                    .body("task_history_list.get(4).update_action",equalTo("Terminate"));
            });


        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response resultCompleteReport = restApiActions.get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    userWithCompletionProcessEnabled.getHeaders()
                );


                resultCompleteReport.prettyPrint();
                resultCompleteReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
                    .body("reportable_task_list.get(0).update_action", equalTo("Terminate"))
                    .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
                    .body("reportable_task_list.get(0).termination_process", equalTo("EXUI_CASE_EVENT_COMPLETION"));
            });

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(userWithCompletionProcessEnabled.getHeaders());
        authorizationProvider.deleteAccount(userWithCompletionProcessEnabled.getAccount().getUsername());
    }


    @Test
    public void user_should_complete_task_and_no_termination_process_recorded_in_replica_tables_when_flag_disabled() {

        TestAuthenticationCredentials userWithCompletionProcessDisabled =
            authorizationProvider.getNewTribunalCaseworker("wa-user-with-completion-process-disabled-");

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("processApplication",
                                                                       "Process Application");
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(userWithCompletionProcessDisabled.getHeaders(),
                                                   "tribunal-caseworker");

        String taskId = taskVariables.getTaskId();
        given.iClaimATaskWithIdAndAuthorization(
            taskId,
            userWithCompletionProcessDisabled.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultReportable = restApiActions.get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    userWithCompletionProcessDisabled.getHeaders()
                );
                resultReportable.prettyPrint();
                resultReportable.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1));
            });

        Response resultComplete = restApiActions.post(
            ENDPOINT_BEING_TESTED_COMPLETE + "?completion_process=" + "EXUI_CASE-EVENT_COMPLETION",
            taskId,
            userWithCompletionProcessDisabled.getHeaders()
        );


        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = restApiActions.get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    userWithCompletionProcessDisabled.getHeaders()
                );
                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(4))
                    .body("task_history_list.get(3).termination_process", nullValue());
            });
        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultCompleteReport = restApiActions.get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    userWithCompletionProcessDisabled.getHeaders()
                );

                resultCompleteReport.prettyPrint();
                resultCompleteReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("COMPLETED"))
                    .body("reportable_task_list.get(0).update_action", equalTo("Complete"))
                    .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
                    .body("reportable_task_list.get(0).termination_process", nullValue());
            });

        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(userWithCompletionProcessDisabled.getHeaders());
        authorizationProvider.deleteAccount(userWithCompletionProcessDisabled.getAccount().getUsername());
    }


    @Test
    public void user_should_cancel_task_when_role_assignment_verification_passed() {
        TestAuthenticationCredentials caseworkerCredentials2 = authorizationProvider.getNewTribunalCaseworker(
            "wa-ft-test-r3-");
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
        common.clearAllRoleAssignments(caseworkerCredentials2.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials2.getAccount().getUsername());
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
