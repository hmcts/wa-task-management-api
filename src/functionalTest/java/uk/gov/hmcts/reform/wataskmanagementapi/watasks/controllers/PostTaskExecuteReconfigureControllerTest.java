package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AwaitilityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CASE_MANAGER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.GIN_INDEX_CASE_WORKER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_CASEWORKER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Import(AwaitilityTestConfig.class)
@Slf4j
public class PostTaskExecuteReconfigureControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";

    TestAuthenticationCredentials caseWorkerWithTribRole;
    TestAuthenticationCredentials ginIndexCaseworkerCredentials;
    TestAuthenticationCredentials userWithCaseManagerRole;
    TestAuthenticationCredentials caseWorkerWithJudgeRole;

    @Before
    public void setUp() {
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_TRIB_CASEWORKER_ROLE);
        userWithCaseManagerRole = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_CASE_MANAGER_ROLE);
        ginIndexCaseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(GIN_INDEX_CASE_WORKER);
        caseWorkerWithJudgeRole = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_JUDGE_ROLE);
    }

    @Test
    public void should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_no_failures_to_report()
        throws Exception {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0)); //Default max results


        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await()
            .untilAsserted(() -> {
                Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "/task/{task-id}",
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE_FAILURES,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_not_reconfigure_task_when_task_validation_fails_during_reconfiguration()
        throws Exception {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "validateMandatoryTaskAttributesDuringReconfiguration",
            "validateMandatoryTaskAttributesDuringReconfiguration"
        );
        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);
        log.info("Assign task  task id {}, case id {}", taskVariables.getTaskId(), taskVariables.getCaseId());

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );
        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );
        result.prettyPrint();

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await()
            .untilAsserted(() -> {
                Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "/task/{task-id}",
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }



    @Test
    public void should_return_200_after_task_marked_but_not_executed_and_failure_process_finds_unprocessed_record()
        throws Exception {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        assignTaskAndValidate(taskVariables,
                              taskFunctionalTestsUserUtils
                                  .getAssigneeId(caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0)); //Default max results

        Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE_FAILURES,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        taskResult.then().assertThat()
            .statusCode(HttpStatus.OK.value()); //Default max results

        // execute reconfigure process is not performed on current task
        // retry window is set 0 hours, so 1 unprocessed reconfiguration record to report
        await()
            .until(() -> {

                Response taskResultAfterReconfigFail = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "/task/{task-id}",
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_recalculate_due_date_when_executed_for_reconfigure() throws Exception {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "calculateDueDate",
            "Calculate Due Date"
        );

        taskFunctionalTestsApiUtils.getCommon().setupStandardCaseManager(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        String taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await()
            .untilAsserted(() -> {
                Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "/task/{task-id}",
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_recalculate_next_hearing_date_using_interval_calculation_when_executed_for_reconfigure() {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "functionalTestTask1",
            "functional Test Task 1"
        );

        taskFunctionalTestsApiUtils.getCommon().setupStandardCaseManager(assignerCredentials.getHeaders(),
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);
        log.info("after initiation assert");

        //update next hearing date
        taskFunctionalTestsApiUtils.getGiven().updateWACcdCase(taskVariables.getCaseId(),
            Map.of("nextHearingDate", "2022-12-02T16:00:00+01:00"),
            "COMPLETE"
        );
        log.info("after update next hearing date");

        assignTaskAndValidate(taskVariables,
                              taskFunctionalTestsUserUtils
                                  .getAssigneeId(caseWorkerWithTribRole.getHeaders()),assignerCredentials);
        log.info("after assign and validate");


        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );
        log.info("after mark to reconfigure");

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        log.info("after reconfiguration execute");
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await()
            .untilAsserted(() -> {

                Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                    "/task/{task-id}",
                    taskId,
                    caseWorkerWithTribRole.getHeaders()
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
            });

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_reconfigure_multiple_additional_properties_after_validation_reconfigure_flag() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(
            additionalProperties,
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "reviewSpecificAccessRequestJudiciary2"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, userWithCaseManagerRole.getHeaders(), additionalProperties);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            userWithCaseManagerRole.getHeaders()
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

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithJudgeRole.getHeaders()),userWithCaseManagerRole);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(
                TaskOperationType.MARK_TO_RECONFIGURE,
                taskVariables.getCaseId()
            ),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithJudgeRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().untilAsserted(() -> {
            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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
                .body(
                    "task.additional_properties",
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void
        should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_default_value_role_assignment_id() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithAdditionalPropertiesAndRetrieveIds(
                additionalProperties,
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "reviewSpecificAccessRequestJudiciary"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, userWithCaseManagerRole.getHeaders(), additionalProperties);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                userWithCaseManagerRole.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithJudgeRole.getHeaders()),userWithCaseManagerRole);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                        taskVariables.getCaseId()),
                caseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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


        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForExecuteReconfiguration(
                        TaskOperationType.EXECUTE_RECONFIGURE,
                        OffsetDateTime.now().minus(Duration.ofDays(1))
                ),
                caseWorkerWithJudgeRole.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());

        await().untilAsserted(() -> {
            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void
        should_return_a_200_after_tasks_are_marked_and_executed_for_reconfigure_no_default_value_role_assignment_id() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithAdditionalPropertiesAndRetrieveIds(
                additionalProperties,
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "reviewSpecificAccessRequestJudiciary1"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, userWithCaseManagerRole.getHeaders(), additionalProperties);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                userWithCaseManagerRole.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.additional_properties", equalToObject(Map.of(
                        "roleAssignmentId", roleAssignmentId)));

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithJudgeRole.getHeaders()),userWithCaseManagerRole);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                        taskVariables.getCaseId()),
                caseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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


        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                taskOperationRequestForExecuteReconfiguration(
                        TaskOperationType.EXECUTE_RECONFIGURE,
                        OffsetDateTime.now().minus(Duration.ofDays(1))
                ),
                caseWorkerWithJudgeRole.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
                .statusCode(HttpStatus.OK.value());

        await().untilAsserted(() -> {
            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_set_next_hearing_date_to_empty_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "endToEndTask",
            "end To End Task"
        );

        taskFunctionalTestsApiUtils.getCommon().setupStandardCaseManager(assignerCredentials.getHeaders(),
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await().untilAsserted(() -> {

            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithTribRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_set_title_to_existing_value_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "taskAttributesWithDefaultValue",
            "Task Attributes With Default Value"
        );

        taskFunctionalTestsApiUtils.getCommon().setupStandardCaseManager(assignerCredentials.getHeaders(),
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await().untilAsserted(() -> {

            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithTribRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_reconfigure_camunda_task_attributes_after_validation_reconfigure_flag() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId
        );
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithAdditionalPropertiesAndRetrieveIds(
            additionalProperties,
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "reconfigTaskAttributesTask"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, userWithCaseManagerRole.getHeaders(), additionalProperties);

        await().untilAsserted(() -> {
            Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                userWithCaseManagerRole.getHeaders()
            );

            result.prettyPrint();
            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.task_title", equalTo("A Task")) //Default task name
                .body(
                    "task.additional_properties", equalTo(Map.of(
                        "key1", "value1",
                        "key2", "value1",
                        "roleAssignmentId", roleAssignmentId
                    ))
            );
        });

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithJudgeRole.getHeaders()),userWithCaseManagerRole);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(
                TaskOperationType.MARK_TO_RECONFIGURE,
                taskVariables.getCaseId()
            ),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());
        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().untilAsserted(() -> {
            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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
                .body(
                    "task.task_title",
                    is("name - " + taskName + " - state - ASSIGNED - category - Protection")
                )
                .body("task.due_date", notNullValue())
                .body("task.role_category", is("CTSC"))
                .body(
                    "task.additional_properties", equalTo(Map.of(
                        "key1", "value1",
                        "key2", "reconfigValue2",
                        "roleAssignmentId", roleAssignmentId
                    ))
            );
        });
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_set_additional_properties_to_null_if_dmn_evaluates_to_empty_when_executed_for_reconfigure() {

        TestAuthenticationCredentials assignerCredentials = authorizationProvider
            .getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "endToEndTask",
            "end To End Task"
        );

        taskFunctionalTestsApiUtils.getCommon().setupStandardCaseManager(assignerCredentials.getHeaders(),
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

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        await().untilAsserted(() -> {

            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithTribRole.getHeaders()
            );

            taskResult.prettyPrint();

            taskResult.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and().contentType(MediaType.APPLICATION_JSON_VALUE)
                .and().body("task.id", equalTo(taskId))
                .body("task.task_state", is("assigned"))
                .body("task.reconfigure_request_time", nullValue())
                .body("task.last_reconfiguration_time", notNullValue())
                .body("task.additional_properties", nullValue())
                .body("task.next_hearing_date", nullValue());
        });

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            taskVariables.getCaseId(),
            "testEndToEndTask",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "/task/search-for-completable",
            decideAnApplicationSearchRequest,
            caseWorkerWithTribRole.getHeaders()
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
        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "/task?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithTribRole.getHeaders()
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_not_send_all_db_attributes_to_reconfigure_camunda_task() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "reconfigTaskAttributesTask2",
            "reconfigTaskAttributesTask2"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, userWithCaseManagerRole.getHeaders());

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            userWithCaseManagerRole.getHeaders()
        );


        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId));

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithJudgeRole.getHeaders()),userWithCaseManagerRole);

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForMarkToReconfigure(TaskOperationType.MARK_TO_RECONFIGURE,
                                                     taskVariables.getCaseId()),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());


        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());
        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequestForExecuteReconfiguration(
                TaskOperationType.EXECUTE_RECONFIGURE,
                OffsetDateTime.now().minus(Duration.ofDays(1))
            ),
            caseWorkerWithJudgeRole.getHeaders()
        );

        result.body().prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        await().untilAsserted(() -> {
            Response taskResult = taskFunctionalTestsApiUtils.getRestApiActions().get(
                "/task/{task-id}",
                taskId,
                caseWorkerWithJudgeRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
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

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId,
                                       TestAuthenticationCredentials assignerCredentials) {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "task/{task-id}/assign",
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE
        );

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned",
            assignerCredentials.getHeaders()
        );
        taskFunctionalTestsApiUtils.getAssertions().taskFieldWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assignee",
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

