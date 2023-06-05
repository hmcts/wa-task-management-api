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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

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
            .body("reportable_task_list.get(0).update_action", equalTo("Configure"));

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
            .body("task_history_list.get(2).updated", notNullValue());

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
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", equalTo(null))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time", equalTo(null))
            .body("reportable_task_list.get(0).handling_time", equalTo(null))
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
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(1));

        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", equalTo(null));

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
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", equalTo(0))
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_time", equalTo(null))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue());

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
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(1));

        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", equalTo(null));

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

    @Test
    public void user_should_complete_task_and_complete_action_recorded_in_replica_tables() {

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
            ENDPOINT_BEING_TESTED_REPORTABLE,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        resultHistory.prettyPrint();
        resultHistory.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("reportable_task_list.size()", equalTo(1))
            .body("reportable_task_list.get(0).state", equalTo("ASSIGNED"))
            .body("reportable_task_list.get(0).assignee", notNullValue())
            .body("reportable_task_list.get(0).updated_by", notNullValue())
            .body("reportable_task_list.get(0).updated", notNullValue())
            .body("reportable_task_list.get(0).update_action", equalTo("Claim"))
            .body("reportable_task_list.get(0).created_date", notNullValue())
            .body("reportable_task_list.get(0).final_state_label", equalTo(null))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", equalTo(null))
            .body("reportable_task_list.get(0).processing_time_days", equalTo(null))
            .body("reportable_task_list.get(0).is_within_sla", equalTo(null))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(null))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue());

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
            .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
            .body("reportable_task_list.get(0).first_assigned_date", notNullValue())
            .body("reportable_task_list.get(0).first_assigned_date_time", notNullValue())
            .body("reportable_task_list.get(0).wait_time_days", notNullValue())
            .body("reportable_task_list.get(0).handling_time_days", notNullValue())
            .body("reportable_task_list.get(0).processing_time_days", notNullValue())
            .body("reportable_task_list.get(0).is_within_sla", equalTo("Yes"))
            .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
            .body("reportable_task_list.get(0).due_date_to_completed_diff_days", equalTo(-1))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue())
            .body("reportable_task_list.get(0).wait_time", notNullValue())
            .body("reportable_task_list.get(0).handling_time", notNullValue())
            .body("reportable_task_list.get(0).processing_time", notNullValue())
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
            .body("reportable_task_list.get(0).completed_date", equalTo(null))
            .body("reportable_task_list.get(0).completed_date_time", equalTo(null))
            .body("reportable_task_list.get(0).final_state_label", equalTo("USER_CANCELLED"))
            .body("reportable_task_list.get(0).wait_time_days", equalTo(null))
            .body("reportable_task_list.get(0).due_date", notNullValue())
            .body("reportable_task_list.get(0).last_updated_date", notNullValue());

        common.cleanUpTask(taskId);
    }
}
