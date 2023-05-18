package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;

@Slf4j
public class PostTaskExecuteReconfigureControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = authorizationProvider.getNewWaTribunalCaseworker(EMAIL_PREFIX_R3_5);
        assigneeCredentials = authorizationProvider.getNewWaTribunalCaseworker(EMAIL_PREFIX_R3_5);
        ginIndexCaseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(assignerCredentials.getHeaders());
        common.clearAllRoleAssignments(assigneeCredentials.getHeaders());
        common.clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());

        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(assigneeCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_after_tasks_are_marked_and_executed_for_reconfigure_no_failures_to_report()
        throws Exception {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");
        common.setupWAOrganisationalRoleAssignment(ginIndexCaseworkerCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        result = restApiActions.post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0)); //Default max results


        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        sleep(3000L);
        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", nullValue())
            .body("task.last_reconfiguration_time", notNullValue());

        //no unprocessed reconfiguration records so no error should report
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE_FAILURES,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }


    @Test
    public void should_return_204_after_task_marked_but_not_executed_and_failure_process_finds_unprocessed_record()
        throws Exception {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        result = restApiActions.post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0)); //Default max results

        // execute reconfigure process is not performed on current task
        // retry window is set 0 hours, so 1 unprocessed reconfiguration record to report
        sleep(10000);
        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE_FAILURES,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value()); //Default max results

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_recalculate_due_date_when_executed_for_reconfigure() throws Exception {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "calculateDueDate",
            "Calculate Due Date"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        sleep(3000L);
        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", nullValue())
            .body("task.last_reconfiguration_time", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.due_date", equalTo(LocalDateTime.of(2022, 10, 25,
                    20, 00, 0, 0)
                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_recalculate_next_hearing_date_using_interval_calculation_when_executed_for_reconfigure()
        throws InterruptedException {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "functionalTestTask1",
            "functional Test Task 1"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        taskId = taskVariables.getTaskId();
        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("functional Test Task 1"))
                .body("task.type", equalTo("functionalTestTask1"))
                .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 7, 14)))
                .body("task.priority_date", equalTo(formatDate(2022, 12, 7, 14)))
                .body("task.due_date", equalTo(formatDate(2023, 1, 17, 18)));
        };

        initiateTask(taskVariables, assertConsumer);
        log.info("after initiation assert");

        //update next hearing date
        given.updateWACcdCase(taskVariables.getCaseId(),
            Map.of("nextHearingDate", "2022-12-02T16:00:00+01:00"),
            "COMPLETE"
        );
        log.info("after update next hearing date");

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));
        log.info("after assign and validate");


        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );
        log.info("after mark to reconfigure");

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());


        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        log.info("after reconfiguration execute");
        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        Thread.sleep(3000L);

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", nullValue())
            .body("task.last_reconfiguration_time", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.due_date", equalTo(formatDate(2023, 1, 17, 18)))
            .body("task.priority_date", notNullValue())
            .body("task.priority_date", equalTo(formatDate(2022, 12, 2, 16)))
            .body("task.next_hearing_date", notNullValue())
            .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 2, 16)));

        common.cleanUpTask(taskId);
    }

    private TaskOperationRequest taskOperationRequestForMarkToReconfigure(TaskOperationType operationName,
                                                                          String caseId) {
        TaskOperation operation = TaskOperation.builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(2)
            .build();
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private TaskOperationRequest taskOperationRequestForExecuteReconfiguration(TaskOperationType operationName,
                                                                               OffsetDateTime reconfigureRequestTime) {
        TaskOperation operation = TaskOperation.builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(2)
            .retryWindowHours(0)
            .build();
        return new TaskOperationRequest(operation, taskFilters(reconfigureRequestTime));
    }

    private List<TaskFilter<?>> taskFilters(OffsetDateTime reconfigureRequestTime) {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", reconfigureRequestTime, TaskFilterOperator.AFTER);
        return List.of(filter);
    }

    private List<TaskFilter<?>> taskFilters(String caseId) {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId) {

        Response result = restApiActions.post(
            "task/{task-id}/assign",
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE
        );

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned",
            assignerCredentials.getHeaders()
        );
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee",
            assigneeId, assignerCredentials.getHeaders()
        );
    }

    @NotNull
    private static String formatDate(int year, int month, int day, int hour) {
        return LocalDateTime.of(year, month, day, hour, 0, 0, 0)
            .atZone(ZoneId.systemDefault()).toOffsetDateTime()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
    }

}

