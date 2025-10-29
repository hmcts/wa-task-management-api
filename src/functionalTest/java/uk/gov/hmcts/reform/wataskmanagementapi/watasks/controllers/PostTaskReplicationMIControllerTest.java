package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED_TASK = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED_HISTORY = "/task/{task-id}/history";
    private static final String ENDPOINT_BEING_TESTED_REPORTABLE = "/task/{task-id}/reportable";
    private static final String ENDPOINT_BEING_TESTED_COMPLETE = "task/{task-id}/complete";
    private static final String ENDPOINT_BEING_TESTED_ASSIGNMENTS = "/task/{task-id}/assignments";
    private static final String ENDPOINT_BEING_TESTED_UNCLAIM = "task/{task-id}/unclaim";
    private static final String ENDPOINT_BEING_TESTED_CANCEL = "task/{task-id}/cancel";

    @Value("${environment}")
    private String environment;

    TestAuthenticationCredentials caseWorkerWithWAOrgRoles;
    TestAuthenticationCredentials caseWorkerWithTribRole;
    TestAuthenticationCredentials userWithTaskSupervisorRole;
    TestAuthenticationCredentials caseWorkerWithCftOrgRoles;
    TestAuthenticationCredentials tribCaseworkerWithCompletionEnabled;
    TestAuthenticationCredentials tribCaseworkerWithCompletionDisabled;
    TestAuthenticationCredentials caseWorkerWithJudgeRole;

    @Before
    public void setUp() {
        caseWorkerWithWAOrgRoles = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_WA_ORG_ROLES2);
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.USER_WITH_TRIB_CASEWORKER_ROLE);
        userWithTaskSupervisorRole = taskFunctionalTestsUserUtils.getTestUser(
            TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_TASK_SUPERVISOR_ROLE);
        caseWorkerWithCftOrgRoles = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_CFT_ORG_ROLES);
        tribCaseworkerWithCompletionEnabled = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_TRIB_ROLE_COMPLETION_ENABLED);
        tribCaseworkerWithCompletionDisabled = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.USER_WITH_TRIB_ROLE_COMPLETION_DISABLED);
        caseWorkerWithJudgeRole = taskFunctionalTestsUserUtils
            .getTestUser(TaskFunctionalTestsUserUtils.CASE_WORKER_WITH_JUDGE_ROLE);
    }

    @Test
    public void user_should_configure_task_and_configure_action_recorded_in_replica_tables() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());
        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithWAOrgRoles.getHeaders()
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
            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithWAOrgRoles.getHeaders()
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
            });

        Response resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders()
        );
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list", empty());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_task_and_claim_action_recorded_in_reportable_task() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        AtomicReference<JsonPath> configureJsonPathEvaluator = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultTaskReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                );

                configureJsonPathEvaluator.set(resultTaskReportable.jsonPath());

                assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get().get(
                        "reportable_task_list.get(0).created"))
                               .isBefore(OffsetDateTime.parse(configureJsonPathEvaluator.get().get(
                                   "reportable_task_list.get(0).updated"))));

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
            });

        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() -> assertNotNull(taskId));

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
                assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get().get(
                        "reportable_task_list.get(0).created"))
                               .isEqual(OffsetDateTime.parse(claimJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).created"))));
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                               .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));
                assertTrue(OffsetDateTime.parse(configureJsonPathEvaluator.get().get(
                        "reportable_task_list.get(0).updated"))
                               .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));
                assertNotEquals(
                    configureJsonPathEvaluator.get().get("reportable_task_list.get(0).updated_by").toString(),
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).updated_by").toString()
                );
                assertTrue(LocalTime.parse(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay() > 1);
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_configure_claim_unclaim_and_reclaim_task_actions_recorded_in_replica_tables() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() -> assertNotNull(taskId));

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        AtomicReference<Response> resultHistory = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultHistory.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                ));

                resultHistory.get().prettyPrint();
                resultHistory.get().then().assertThat()
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
            });

        AtomicReference<Response> resultReportable = new AtomicReference<>();
        AtomicReference<JsonPath> claimJsonPathEvaluator = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultReportable.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                ));

                resultReportable.get().prettyPrint();
                resultReportable.get().then().assertThat()
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

                claimJsonPathEvaluator.set(resultReportable.get().jsonPath());
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created"))
                               .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get().get(
                                   "reportable_task_list.get(0).updated"))));
                assertTrue(LocalTime.parse(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay() > 1);
            });

        Response resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );
        resultAssignments.prettyPrint();
        resultAssignments.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1));

        JsonPath resAssignmentsJsonPathEvaluator = resultAssignments.jsonPath();
        JsonPath resHistoryJsonPathEvaluator = resultHistory.get().jsonPath();

        assertEquals(
            resHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
            resAssignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString()
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_UNCLAIM,
            taskId,
            userWithTaskSupervisorRole.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        AtomicReference<Response> resultAssignmentsUnclaim = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultAssignmentsUnclaim.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_ASSIGNMENTS,
                    taskId,
                    userWithTaskSupervisorRole.getHeaders()
                ));
                resultAssignmentsUnclaim.get().prettyPrint();
                resultAssignmentsUnclaim.get().then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_assignments_list.size()", equalTo(1))
                    .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
                    .body("task_assignments_list.get(0).assignment_start", notNullValue())
                    .body("task_assignments_list.get(0).assignment_end", notNullValue());

            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultUnclaimHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    userWithTaskSupervisorRole.getHeaders()
                );

                resultUnclaimHistory.prettyPrint();
                resultUnclaimHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(4));

                JsonPath resUnClaimAssignmentsJsonPathEvaluator = resultAssignmentsUnclaim.get().jsonPath();
                JsonPath resUnClaimHistoryJsonPathEvaluator = resultUnclaimHistory.jsonPath();

                assertEquals(
                    resUnClaimHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                    resUnClaimAssignmentsJsonPathEvaluator
                        .get("task_assignments_list.get(0).assignment_start").toString()
                );
                assertEquals(
                    resUnClaimHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                    resUnClaimAssignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString()
                );
            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                resultReportable.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    userWithTaskSupervisorRole.getHeaders()
                ));

                resultReportable.get().prettyPrint();
                resultReportable.get().then().assertThat()
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

            });

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        Response resultAssignmentsClaim = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response reClaimResultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                );

                reClaimResultHistory.prettyPrint();
                reClaimResultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(5));

                JsonPath assignmentsJsonPathEvaluator = resultAssignmentsClaim.jsonPath();
                JsonPath resultHistoryJsonPathEvaluator = reClaimResultHistory.jsonPath();

                assertEquals(
                    resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                    assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString()
                );
                assertEquals(
                    resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                    assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString()
                );
                assertEquals(
                    resultHistoryJsonPathEvaluator.get("task_history_list.get(4).updated").toString(),
                    assignmentsJsonPathEvaluator.get("task_assignments_list.get(1).assignment_start").toString()
                );
            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
                               .isBefore(OffsetDateTime.parse(reClaimJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get().get("reportable_task_list.get(0).updated"))
                               .isBefore(OffsetDateTime.parse(reClaimJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created").toString(),
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time_days").toString(),
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time").toString(),
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date").toString(),
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString()
                );
                assertTrue(LocalTime.parse(
                    reClaimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay() > 1);
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_and_delete_task_action_recorded_in_replicate_db() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        AtomicReference<Response> resultReportable = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultReportable.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithCftOrgRoles.getHeaders()
                ));

                resultReportable.get().prettyPrint();
                resultReportable.get().then().assertThat()
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
            });

        JsonPath claimJsonPathEvaluator = resultReportable.get().jsonPath();
        assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                       .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator
                                                          .get("reportable_task_list.get(0).updated"))));

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response resultDelete = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        AtomicReference<Response> resultHistory = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultHistory.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithCftOrgRoles.getHeaders()
                ));

                resultHistory.get().prettyPrint();
                resultHistory.get().then().assertThat()
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

            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultDeleteReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithCftOrgRoles.getHeaders()
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

                assertEquals(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).created").toString(),
                    deleteJsonPathEvaluator.get("reportable_task_list.get(0).created").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString(),
                    deleteJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                    deleteJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                    deleteJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString(),
                    deleteJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString()
                );
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).created"))
                               .isEqual(OffsetDateTime.parse(deleteJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).created"))));
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get("reportable_task_list.get(0).updated"))
                               .isBefore(OffsetDateTime.parse(deleteJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));

            });

        Response resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
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
        JsonPath resultHistoryJsonPathEvaluator = resultHistory.get().jsonPath();

        assertEquals(
            resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
            assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString()
        );
        assertEquals(
            resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
            assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString()
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }


    @Test
    public void user_should_claim_complete_task_and_complete_action_recorded_in_replica_tables() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        Awaitility.await().atLeast(3, TimeUnit.SECONDS).pollDelay(3, TimeUnit.SECONDS)
            .untilAsserted(() -> assertNotNull(taskId));

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        AtomicReference<JsonPath> claimJsonPathEvaluator = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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

                claimJsonPathEvaluator.set(resultReportable.jsonPath());
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created"))
                               .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get().get(
                                   "reportable_task_list.get(0).updated"))));
                assertTrue(LocalTime.parse(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay() > 1);

            });

        Response resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
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
            .untilAsserted(() -> assertNotNull(taskId));

        Response resultComplete = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_COMPLETE,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response resultDelete = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseWorkerWithTribRole.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        AtomicReference<Response> resultHistory = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultHistory.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                ));
                resultHistory.get().prettyPrint();
                resultHistory.get().then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(5));

            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultCompleteReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                );

                resultCompleteReport.prettyPrint();
                resultCompleteReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
                    .body("reportable_task_list.get(0).assignee", notNullValue())
                    .body("reportable_task_list.get(0).updated_by", notNullValue())
                    .body("reportable_task_list.get(0).updated", notNullValue())
                    .body("reportable_task_list.get(0).update_action", equalTo("Terminate"))
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

                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created").toString(),
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).created").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time_days").toString(),
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time").toString(),
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date").toString(),
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString()
                );
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get().get("reportable_task_list.get(0).updated"))
                               .isBefore(OffsetDateTime.parse(completeJsonPathEvaluator.get(
                                   "reportable_task_list.get(0).updated"))));

                int waitTimeSeconds = LocalTime.parse(
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay();
                int processingTimeSeconds = LocalTime.parse(
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).processing_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay();
                int handlingTimeSeconds = LocalTime.parse(
                    completeJsonPathEvaluator.get("reportable_task_list.get(0).handling_time").toString(),
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                ).toSecondOfDay();
                assertTrue(waitTimeSeconds > 1);
                assertTrue(processingTimeSeconds > 1);
                assertEquals(handlingTimeSeconds, processingTimeSeconds - waitTimeSeconds);

            });

        resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
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
        JsonPath resultHistoryJsonPathEvaluator = resultHistory.get().jsonPath();

        assertEquals(
            resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
            assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString()
        );
        assertEquals(
            resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
            assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString()
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_claim_complete_terminate_task_and_actions_recorded_in_replica_tables() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        AtomicReference<JsonPath> claimJsonPathEvaluator = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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

                claimJsonPathEvaluator.set(resultReportable.jsonPath());
                assertTrue(OffsetDateTime.parse(claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created"))
                               .isBefore(OffsetDateTime.parse(claimJsonPathEvaluator.get().get(
                                   "reportable_task_list.get(0).updated"))));

            });

        Response resultAssignments = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        Response resultComplete = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_COMPLETE,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response resultDelete = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            caseWorkerWithTribRole.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        AtomicReference<Response> resultHistory = new AtomicReference<>();

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                resultHistory.set(taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                ));

                resultHistory.get().prettyPrint();
                resultHistory.get().then().assertThat()
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
            });


        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {

            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultTerminateReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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

                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).created").toString(),
                    terminateJsonPathEvaluator.get("reportable_task_list.get(0).created").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time_days").toString(),
                    terminateJsonPathEvaluator.get("reportable_task_list.get(0).wait_time_days").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).wait_time").toString(),
                    terminateJsonPathEvaluator.get("reportable_task_list.get(0).wait_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date_time").toString(),
                    terminateJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date_time").toString()
                );
                assertEquals(
                    claimJsonPathEvaluator.get().get("reportable_task_list.get(0).first_assigned_date").toString(),
                    terminateJsonPathEvaluator.get("reportable_task_list.get(0).first_assigned_date").toString()
                );

            });

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {

                Response resultAssignmentsPostTermination = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_ASSIGNMENTS,
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
                );
                resultAssignmentsPostTermination.then().assertThat()
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

                JsonPath assignmentsJsonPathEvaluator = resultAssignmentsPostTermination.jsonPath();
                JsonPath resultHistoryJsonPathEvaluator = resultHistory.get().jsonPath();

                assertEquals(
                    resultHistoryJsonPathEvaluator.get("task_history_list.get(2).updated").toString(),
                    assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_start").toString()
                );
                assertEquals(
                    resultHistoryJsonPathEvaluator.get("task_history_list.get(3).updated").toString(),
                    assignmentsJsonPathEvaluator.get("task_assignments_list.get(0).assignment_end").toString()
                );
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void user_should_complete_task_and_termination_process_recorded_in_replica_tables() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            tribCaseworkerWithCompletionEnabled.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    tribCaseworkerWithCompletionEnabled.getHeaders()
                );


                resultReportable.prettyPrint();
                resultReportable.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1));
            });


        Response resultComplete = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_COMPLETE + "?completion_process=" + "EXUI_CASE-EVENT_COMPLETION",
            taskId,
            tribCaseworkerWithCompletionEnabled.getHeaders()
        );

        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        Response resultTerminate = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            terminateTaskRequest,
            tribCaseworkerWithCompletionEnabled.getHeaders()
        );
        resultTerminate.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_TASK,
            taskId,
            tribCaseworkerWithCompletionEnabled.getHeaders()
        );
        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task.termination_process", equalTo("EXUI_CASE-EVENT_COMPLETION"))
            .body("task.id", equalTo(taskId));

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    tribCaseworkerWithCompletionEnabled.getHeaders()
                );

                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(5))
                    .body("task_history_list.get(3).termination_process",
                          equalTo("EXUI_CASE_EVENT_COMPLETION"))
                    .body("task_history_list.get(3).update_action", equalTo("Complete"))
                    .body("task_history_list.get(4).update_action",equalTo("Terminate"));
            });


        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultCompleteReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    tribCaseworkerWithCompletionEnabled.getHeaders()
                );


                resultCompleteReport.prettyPrint();
                resultCompleteReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
                    .body("reportable_task_list.get(0).update_action", equalTo("Terminate"))
                    .body("reportable_task_list.get(0).final_state_label", equalTo("COMPLETED"))
                    .body("reportable_task_list.get(0).termination_process",
                          equalTo("EXUI_CASE_EVENT_COMPLETION"));
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(tribCaseworkerWithCompletionEnabled.getHeaders());
    }


    @Test
    public void user_should_complete_task_and_no_termination_process_recorded_in_replica_tables_when_flag_disabled() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            tribCaseworkerWithCompletionDisabled.getHeaders(),
            "tribunal-caseworker"
        );

        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            tribCaseworkerWithCompletionDisabled.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultReportable = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    tribCaseworkerWithCompletionDisabled.getHeaders()
                );
                resultReportable.prettyPrint();
                resultReportable.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1));
            });

        Response resultComplete = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_COMPLETE + "?completion_process=" + "EXUI_CASE-EVENT_COMPLETION",
            taskId,
            tribCaseworkerWithCompletionDisabled.getHeaders()
        );


        resultComplete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response resultDelete = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            tribCaseworkerWithCompletionDisabled.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    tribCaseworkerWithCompletionDisabled.getHeaders()
                );
                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(5))
                    .body("task_history_list.get(3).termination_process", nullValue())
                    .body("task_history_list.get(4).termination_process", nullValue());
            });
        await()
            .atLeast(3, TimeUnit.SECONDS)
            .pollDelay(3, TimeUnit.SECONDS)
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                Response resultCompleteReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    tribCaseworkerWithCompletionDisabled.getHeaders()
                );

                resultCompleteReport.prettyPrint();
                resultCompleteReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
                    .body("reportable_task_list.get(0).update_action", equalTo("AutoCancel"))
                    .body("reportable_task_list.get(0).final_state_label", equalTo("AUTO_CANCELLED"))
                    .body("reportable_task_list.get(0).termination_process", nullValue());
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(tribCaseworkerWithCompletionDisabled.getHeaders());
    }


    @Test
    public void user_should_cancel_task_when_role_assignment_verification_passed() {
        TestAuthenticationCredentials leadJudgeForSpecificAccess =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "reviewSpecificAccessRequestJudiciary",
            "Review Specific Access Request Judiciary"
        );

        taskFunctionalTestsApiUtils.getCommon().setupLeadJudgeForSpecificAccess(
            leadJudgeForSpecificAccess.getHeaders(),
            taskVariables.getCaseId(),
            WA_JURISDICTION
        );

        initiateTask(taskVariables, caseWorkerWithJudgeRole.getHeaders());

        String taskId = taskVariables.getTaskId();
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_CANCEL,
            taskId,
            leadJudgeForSpecificAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        Response resultDelete = taskFunctionalTestsApiUtils.getRestApiActions().delete(
            ENDPOINT_BEING_TESTED_TASK,
            taskVariables.getTaskId(),
            terminateTaskRequest,
            leadJudgeForSpecificAccess.getHeaders()
        );

        resultDelete.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    leadJudgeForSpecificAccess.getHeaders()
                );

                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(4))
                    .body("task_history_list.get(2).state", equalTo("CANCELLED"))
                    .body("task_history_list.get(2).assignee", equalTo(null))
                    .body("task_history_list.get(2).updated_by", notNullValue())
                    .body("task_history_list.get(2).updated", notNullValue())
                    .body("task_history_list.get(2).update_action", equalTo("Cancel"))
                    .body("task_history_list.get(2).due_date_time", notNullValue())
                    .body("task_history_list.get(2).updated", notNullValue())
                    .body("task_history_list.get(2).completed_date", nullValue())
                    .body("task_history_list.get(2).completed_date_time", nullValue())
                    .body("task_history_list.get(2).wait_time_days", nullValue())
                    .body("task_history_list.get(2).wait_time", nullValue())
                    .body("task_history_list.get(2).first_assigned_date", nullValue())
                    .body("task_history_list.get(2).first_assigned_date_time", nullValue());



                Response resultCancelReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    leadJudgeForSpecificAccess.getHeaders()
                );

                resultCancelReport.prettyPrint();
                resultCancelReport.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("reportable_task_list.size()", equalTo(1))
                    .body("reportable_task_list.get(0).state", equalTo("TERMINATED"))
                    .body("reportable_task_list.get(0).assignee", equalTo(null))
                    .body("reportable_task_list.get(0).updated_by", notNullValue())
                    .body("reportable_task_list.get(0).updated", notNullValue())
                    .body("reportable_task_list.get(0).update_action", equalTo("AutoCancel"))
                    .body("reportable_task_list.get(0).due_date", notNullValue())
                    .body("reportable_task_list.get(0).last_updated_date", notNullValue())
                    .body("reportable_task_list.get(0).completed_date", nullValue())
                    .body("reportable_task_list.get(0).completed_date_time", nullValue())
                    .body("reportable_task_list.get(0).final_state_label", equalTo("AUTO_CANCELLED"))
                    .body("reportable_task_list.get(0).number_of_reassignments", equalTo(0))
                    .body("reportable_task_list.get(0).wait_time_days", nullValue())
                    .body("reportable_task_list.get(0).wait_time", nullValue())
                    .body("reportable_task_list.get(0).first_assigned_date", nullValue())
                    .body("reportable_task_list.get(0).first_assigned_date_time", nullValue());

            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(leadJudgeForSpecificAccess.getHeaders());
        authorizationProvider.deleteAccount(leadJudgeForSpecificAccess.getAccount().getUsername());
    }

    @Test
    public void user_should_configure_claim_unclaim_multiple_times_for_reassignments_check() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );
        initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(caseWorkerWithTribRole.getHeaders(), "task-supervisor");
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED_UNCLAIM,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Response resultAssignmentsUnclaim = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(1))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"));

        TestAuthenticationCredentials caseWorkerWithTribRole2 =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(caseWorkerWithTribRole2.getHeaders(), "tribunal-caseworker");

        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole2.getHeaders(),
            HttpStatus.NO_CONTENT
        );

        resultAssignmentsUnclaim = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole2.getHeaders()
        );
        resultAssignmentsUnclaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"));

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(caseWorkerWithTribRole2.getHeaders(), "tribunal-caseworker");
        taskFunctionalTestsApiUtils.getGiven().iClaimATaskWithIdAndAuthorization(
            taskId,
            caseWorkerWithTribRole2.getHeaders(),
            HttpStatus.NO_CONTENT
        );


        Response resultAssignmentsClaim = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED_ASSIGNMENTS,
            taskId,
            caseWorkerWithTribRole2.getHeaders()
        );

        resultAssignmentsClaim.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task_assignments_list.size()", equalTo(2))
            .body("task_assignments_list.get(0).assignment_end_reason", equalTo("UNCLAIMED"))
            .body("task_assignments_list.get(1).assignment_end_reason", nullValue());

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultHistory = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_HISTORY,
                    taskId,
                    caseWorkerWithTribRole2.getHeaders()
                );

                resultHistory.prettyPrint();
                resultHistory.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .body("task_history_list.size()", equalTo(6));

            });

        await()
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                Response resultReport = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    ENDPOINT_BEING_TESTED_REPORTABLE,
                    taskId,
                    caseWorkerWithTribRole2.getHeaders()
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
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseWorkerWithTribRole2.getHeaders());
        authorizationProvider.deleteAccount(caseWorkerWithTribRole2.getAccount().getUsername());
    }
}
