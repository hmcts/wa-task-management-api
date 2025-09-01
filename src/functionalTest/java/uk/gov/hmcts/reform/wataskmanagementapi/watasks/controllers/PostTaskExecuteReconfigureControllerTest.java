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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;

@Slf4j
public class PostTaskExecuteReconfigureControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";

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
    public void should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_no_failures_to_report()
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
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

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
            .statusCode(HttpStatus.OK.value());

        await()
            .atLeast(5, TimeUnit.SECONDS)
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue());
            });

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
            .statusCode(HttpStatus.OK.value());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_not_reconfigure_task_when_task_validation_fails_during_reconfiguration()
        throws Exception {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "validateMandatoryTaskAttributesDuringReconfiguration",
            "validateMandatoryTaskAttributesDuringReconfiguration"
        );
        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(),
                                                       taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );

        initiateTask(taskVariables);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));
        log.info("Assign task  task id {}, case id {}", taskVariables.getTaskId(), taskVariables.getCaseId());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );
        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );
        result.prettyPrint();

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await()
            .atLeast(5, TimeUnit.SECONDS)
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", notNullValue())
                    .body("task.last_reconfiguration_time", nullValue());
            });
        common.cleanUpTask(taskId);
    }



    @Test
    public void should_return_200_after_task_marked_but_not_executed_and_failure_process_finds_unprocessed_record()
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
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

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

        Response taskResult = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE_FAILURES,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        taskResult.then().assertThat()
            .statusCode(HttpStatus.OK.value()); //Default max results

        // execute reconfigure process is not performed on current task
        // retry window is set 0 hours, so 1 unprocessed reconfiguration record to report
        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .until(() -> {

                Response taskResultAfterReconfigFail = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResultAfterReconfigFail.prettyPrint();

                taskResultAfterReconfigFail.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", notNullValue())
                    .body("task.last_reconfiguration_time", nullValue());
                return true;
            });

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
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

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
            .statusCode(HttpStatus.OK.value());

        await()
            .atLeast(5, TimeUnit.SECONDS)
            .pollDelay(5, TimeUnit.SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.due_date", notNullValue())
                    .body("task.due_date", equalTo(LocalDateTime.of(2025, 10, 23,
                                                                    20, 00, 0, 0)
                                                       .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                       .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
            });

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_recalculate_next_hearing_date_using_interval_calculation_when_executed_for_reconfigure() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "functionalTestTask1",
            "functional Test Task 1"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        String taskId = taskVariables.getTaskId();
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
                .body("task.due_date", equalTo(formatDate(2026, 1, 15, 18)));
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
            .statusCode(HttpStatus.OK.value());


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
            .statusCode(HttpStatus.OK.value());


        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .until(() -> {

                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.due_date", notNullValue())
                    .body("task.due_date", equalTo(formatDate(2026, 1, 15, 18)))
                    .body("task.priority_date", notNullValue())
                    .body("task.priority_date", equalTo(formatDate(2022, 12, 2, 16)))
                    .body("task.next_hearing_date", notNullValue())
                    .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 2, 16)));

                return true;
            });

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_reconfigure_multiple_additional_properties_after_validation_reconfigure_flag() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(
            additionalProperties,
            "requests/ccd/wa_case_data.json",
            "reviewSpecificAccessRequestJudiciary2"
        );
        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(assignerCredentials.getHeaders(), "case-manager");

        initiateTask(taskVariables, assignerCredentials.getHeaders(), additionalProperties);

        Response result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assignerCredentials.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body(
                "task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4",
                    "key5", "value5",
                    "key6", "value6",
                    "roleAssignmentId", roleAssignmentId

                ))
        );

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "judge");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(
                TaskOperationType.MARK_TO_RECONFIGURE,
                taskVariables.getCaseId()
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


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
            .body("task.role_category", equalTo("JUDICIAL"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue())
            .body(
                "task.additional_properties",
                equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4",
                    "key5", "value5",
                    "key6", "value6",
                    "roleAssignmentId", roleAssignmentId

                ))
        );

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.role_category", equalTo("ADMIN"))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.additional_properties",
                          /* As per camunda configuration below are canReconfigure and evaluated
                                    Reconfiguration values
                                    key1 canReconfigure true value taskAttributes.key1
                                    key2 canReconfigure true and value "updatedvalue2"
                                    key3 canReconfigure false and value updatedvalue3 but
                                        as canReconfigure is set to false it should not reconfigure
                                    key4 canReconfigure true and value empty
                                    key5 canReconfigure empty and  value "updatedvalue5"" but
                                        as canReconfigure is set to empty it should not reconfigure
                                    key6 canReconfigure true and value null so can see in db that it is set to null
                                    key7 canReconfigure true, initial value null and reconfigured value "updatedvalue7"
                          */
                          equalTo(Map.of(
                            "key1", "value1",
                            "key2", "updatedvalue2",
                            "key3", "value3",
                            "key4", "",
                            "key5", "value5",
                            "key7", "updatedvalue7",
                            "roleAssignmentId", roleAssignmentId
                        ))
                );
            });
        common.cleanUpTask(taskId);
    }

    @Test
    public void
        should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_default_value_role_assignment_id() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(
                additionalProperties,
                "requests/ccd/wa_case_data.json",
                "reviewSpecificAccessRequestJudiciary"
        );
        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(assignerCredentials.getHeaders(), "case-manager");

        initiateTask(taskVariables, assignerCredentials.getHeaders(), additionalProperties);

        Response result = restApiActions.get(
                "/task/{task-id}",
                taskId,
                assignerCredentials.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "judge");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                        taskVariables.getCaseId()),
                assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());


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
                .body("task.last_reconfiguration_time", nullValue())
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));


        result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForExecuteReconfiguration(
                        TaskOperationType.EXECUTE_RECONFIGURE,
                        OffsetDateTime.now().minus(Duration.ofDays(1))
                ),
                assigneeCredentials.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());

        await().ignoreException(Exception.class)
                .atLeast(5, TimeUnit.SECONDS)
                .pollInterval(5, SECONDS)
                .atMost(180, SECONDS)
                .untilAsserted(() -> {
                    Response taskResult = restApiActions.get(
                            "/task/{task-id}",
                            taskId,
                            assigneeCredentials.getHeaders()
                    );

                    taskResult.prettyPrint();

                    taskResult.then().assertThat()
                            .statusCode(HttpStatus.OK.value())
                            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                            .and().body("task.id", equalTo(taskId))
                            .body("task.task_state", is("assigned"))
                            .body("task.reconfigure_request_time", nullValue())
                            .body("task.last_reconfiguration_time", notNullValue())
                            .body("task.additional_properties", equalToObject(Map.of(
                                    "roleAssignmentId", roleAssignmentId)));
                });
        common.cleanUpTask(taskId);
    }

    @Test
    public void
        should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_no_default_value_role_assignment_id() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(
                additionalProperties,
                "requests/ccd/wa_case_data.json",
                "reviewSpecificAccessRequestJudiciary1"
        );
        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(assignerCredentials.getHeaders(), "case-manager");

        initiateTask(taskVariables, assignerCredentials.getHeaders(), additionalProperties);

        Response result = restApiActions.get(
                "/task/{task-id}",
                taskId,
                assignerCredentials.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "judge");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                        taskVariables.getCaseId()),
                assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());


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
                .body("task.last_reconfiguration_time", nullValue())
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));


        result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForExecuteReconfiguration(
                        TaskOperationType.EXECUTE_RECONFIGURE,
                        OffsetDateTime.now().minus(Duration.ofDays(1))
                ),
                assigneeCredentials.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());

        await().ignoreException(Exception.class)
                .atLeast(5, TimeUnit.SECONDS)
                .pollInterval(5, SECONDS)
                .atMost(180, SECONDS)
                .untilAsserted(() -> {
                    Response taskResult = restApiActions.get(
                            "/task/{task-id}",
                            taskId,
                            assigneeCredentials.getHeaders()
                    );

                    taskResult.prettyPrint();

                    taskResult.then().assertThat()
                            .statusCode(HttpStatus.OK.value())
                            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                            .and().body("task.id", equalTo(taskId))
                            .body("task.task_state", is("assigned"))
                            .body("task.reconfigure_request_time", nullValue())
                            .body("task.last_reconfiguration_time", notNullValue())
                            .body("task.additional_properties", equalToObject(Map.of(
                                    "roleAssignmentId", roleAssignmentId)));
                });
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_set_next_hearing_date_to_empty_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "endToEndTask",
            "end To End Task"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
                                        taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        String taskId = taskVariables.getTaskId();
        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("end To End Task"))
                .body("task.type", equalTo("endToEndTask"))
                .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 7, 14)));
        };

        initiateTask(taskVariables, assertConsumer);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

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
            .body("task.last_reconfiguration_time", nullValue())
            .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 7, 14)));

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {

                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.next_hearing_date", nullValue());
            });
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_set_title_to_existing_value_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "taskAttributesWithDefaultValue",
            "Task Attributes With Default Value"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
                                        taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        String taskId = taskVariables.getTaskId();
        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Task Attributes With Default Value"))
                .body("task.type", equalTo("taskAttributesWithDefaultValue"));
        };

        initiateTask(taskVariables, assertConsumer);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

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
            .body("task.task_title", equalTo("Task Title"))
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
            .statusCode(HttpStatus.OK.value());


        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {

                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.task_title", equalTo("Task Title"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue());
            });
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_reconfigure_camunda_task_attributes_after_validation_reconfigure_flag() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );
        TestVariables taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(
            additionalProperties,
            "requests/ccd/wa_case_data.json",
            "reconfigTaskAttributesTask"
        );
        String taskId = taskVariables.getTaskId();
        common.setupWAOrganisationalRoleAssignment(assignerCredentials.getHeaders(), "case-manager");

        initiateTask(taskVariables, assignerCredentials.getHeaders(), additionalProperties);

        Response result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assignerCredentials.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_title", equalTo("A Task")) //Default task name
            .body("task.additional_properties", equalTo(Map.of(
                "key1", "value1",
                "key2", "value1",
                "roleAssignmentId", roleAssignmentId
            )));

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "judge");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                                                     taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


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

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();
                String taskName = taskVariables.getTaskName();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.security_classification", is("PUBLIC"))
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.task_title",
                          is("name - " + taskName + " - state - ASSIGNED - category - Protection"))
                    .body("task.due_date", notNullValue())
                    .body("task.role_category", is("CTSC"))
                    .body("task.additional_properties", equalTo(Map.of(
                        "key1", "value1",
                        "key2", "reconfigValue2",
                        "roleAssignmentId", roleAssignmentId
                    )));
            });
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_set_additional_properties_to_null_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "endToEndTask",
            "end To End Task"
        );

        common.setupStandardCaseManager(assignerCredentials.getHeaders(),
                                        taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        String taskId = taskVariables.getTaskId();
        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("end To End Task"))
                .body("task.type", equalTo("endToEndTask"))
                .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 7, 14)));
        };

        initiateTask(taskVariables, assertConsumer);

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

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
            .body("task.last_reconfiguration_time", nullValue())
            .body("task.next_hearing_date", equalTo(formatDate(2022, 12, 7, 14)));

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {

                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.additional_properties",nullValue())
                    .body("task.next_hearing_date", nullValue());
            });

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            taskVariables.getCaseId(),
            "testEndToEndTask",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );

        result = restApiActions.post(
            "/task/search-for-completable",
            decideAnApplicationSearchRequest,
            assigneeCredentials.getHeaders()
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("tasks.size()", greaterThanOrEqualTo(1))
            .body("tasks.id", everyItem(is(equalTo(taskId))))
            .body("tasks.task_state", everyItem(is(equalTo("assigned"))))
            .body("tasks.reconfigure_request_time", everyItem(is(nullValue())))
            .body("tasks.last_reconfiguration_time", everyItem(is(notNullValue())))
            .body("tasks.additional_properties",everyItem(is(nullValue())))
            .body("tasks.next_hearing_date", everyItem(is(nullValue())));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList(WA_JURISDICTION)),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));
        result = restApiActions.post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().body("tasks.size()", greaterThanOrEqualTo(1))
            .body("tasks.id", everyItem(is(equalTo(taskId))))
            .body("tasks.task_state", everyItem(is(equalTo("assigned"))))
            .body("tasks.reconfigure_request_time", everyItem(is(nullValue())))
            .body("tasks.last_reconfiguration_time", everyItem(is(notNullValue())))
            .body("tasks.additional_properties",everyItem(is(nullValue())))
            .body("tasks.next_hearing_date", everyItem(is(nullValue())));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_not_send_all_db_attributes_to_reconfigure_camunda_task() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds(
            "reconfigTaskAttributesTask2",
            "reconfigTaskAttributesTask2"
        );
        String taskId = taskVariables.getTaskId();

        common.setupWAOrganisationalRoleAssignment(assignerCredentials.getHeaders(), "case-manager");

        initiateTask(taskVariables, assignerCredentials.getHeaders());

        Response result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assignerCredentials.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId));

        common.setupWAOrganisationalRoleAssignment(assigneeCredentials.getHeaders(), "judge");

        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                                                     taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


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

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().ignoreException(Exception.class)
            .atLeast(5, TimeUnit.SECONDS)
            .pollInterval(5, SECONDS)
            .atMost(180, SECONDS)
            .untilAsserted(() -> {
                Response taskResult = restApiActions.get(
                    "/task/{task-id}",
                    taskId,
                    assigneeCredentials.getHeaders()
                );

                taskResult.prettyPrint();

                taskResult.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                    .and().body("task.id", equalTo(taskId))
                    .body("task.task_state", is("assigned"))
                    .body("task.reconfigure_request_time", nullValue())
                    .body("task.last_reconfiguration_time", notNullValue())
                    .body("task.task_title", is("Title"))
                    .body("task.work_type_id", is("hearing_work"))
                    .body("task.role_category", is("JUDICIAL"));
            });
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

