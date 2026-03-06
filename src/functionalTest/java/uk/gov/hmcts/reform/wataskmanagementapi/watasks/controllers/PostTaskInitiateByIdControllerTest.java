package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.MANDATORY_FIELD_MISSING_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.WA_JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CFTC_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_GET_ROLES_ENDPOINT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_INITIATION_ENDPOINT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES2;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskInitiateByIdControllerTest {

    @Autowired
    protected AuthorizationProvider authorizationProvider;

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    TestAuthenticationCredentials caseWorkerWithWAOrgRoles;
    TestAuthenticationCredentials caseWorkerWithJudgeRole;
    TestAuthenticationCredentials userWithCFTCtscRole;

    @Before
    public void setUp() {
        caseWorkerWithWAOrgRoles = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_WA_ORG_ROLES2);
        caseWorkerWithJudgeRole = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_JUDGE_ROLE);
        userWithCFTCtscRole = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_CFTC_ROLE);
    }

    @Test
    public void should_return_a_200_when_initiating_a_process_application_task_by_id() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "processApplication", "Process Application"
            );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Process Application"))
                .body("task.type", equalTo("processApplication"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Process Application"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.description",
                    equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                            + "trigger/decideAnApplication)"))
                .body("task.permissions.values.size()", equalTo(6))
                .body("task.permissions.values",
                      hasItems("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))).body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body(
                    "task.priority_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );


        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
            TASK_GET_ROLES_ENDPOINT,
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("roles.values.size()", equalTo(10))
            .body("roles[0].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[0].role_name", equalTo("case-manager"))
            .body("roles[0].permissions", hasItems("Own"))
            .body("roles[1].role_category", equalTo("ADMIN"))
            .body("roles[1].role_name", equalTo("challenged-access-admin"))
            .body("roles[1].permissions", hasItems("Execute"))
            .body("roles[2].role_category", equalTo("JUDICIAL"))
            .body("roles[2].role_name", equalTo("challenged-access-judiciary"))
            .body("roles[2].permissions", hasItems("Read"))
            .body("roles[3].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[3].role_name", equalTo("challenged-access-legal-ops"))
            .body("roles[3].permissions", hasItems("Manage"))
            .body("roles[4].role_category", equalTo("JUDICIAL"))
            .body("roles[4].role_name", equalTo("ftpa-judge"))
            .body("roles[4].permissions", hasItems("Execute"))
            .body("roles[5].role_category", equalTo("JUDICIAL"))
            .body("roles[5].role_name", equalTo("hearing-panel-judge"))
            .body("roles[5].permissions", hasItems("Manage"))
            .body("roles[6].role_category", equalTo("JUDICIAL"))
            .body("roles[6].role_name", equalTo("lead-judge"))
            .body("roles[6].permissions", hasItems("Read"))
            .body("roles[7].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[7].role_name", equalTo("senior-tribunal-caseworker"))
            .body("roles[7].permissions", hasItems("Read", "Execute"))
            .body("roles[8].role_name", equalTo("task-supervisor"))
            .body("roles[8].permissions", hasItems("Read", "Manage", "Cancel"))
            .body("roles[9].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[9].role_name", equalTo("tribunal-caseworker"))
            .body("roles[9].permissions", hasItems("Read", "Own"));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_assign_task_when_assignee_has_right_roles() {

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "assigneeTestTask", "Assignee Test Task"
            );
        String taskId = taskVariables.getTaskId();

        TestAuthenticationCredentials assigneeCaseWorker =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmailAndStaticID("taskassignee.test");

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccessWithAuthorizations(
            assigneeCaseWorker.getHeaders(), taskVariables.getCaseId(), TaskFunctionalTestConstants.WA_JURISDICTION,
            TaskFunctionalTestConstants.WA_CASE_TYPE);

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Assignee Test Task"))
                .body("task.type", equalTo("assigneeTestTask"))
                .body("task.task_state", equalTo("assigned"))
                .body("task.assignee", equalTo("dd8b6c25-6079-36f2-ba0c-afd189308b68"));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, assigneeCaseWorker.getHeaders(), assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "assigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assigneeCaseWorker.getHeaders());
        authorizationProvider.deleteAccount(assigneeCaseWorker.getAccount().getUsername());
    }

    @Test
    public void should_not_assign_task_when_assignee_have_incorrect_roles() {

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "incorrectRoleAssigneeTestTask", "Incorrect RoleAssignment Task Test"
            );
        String taskId = taskVariables.getTaskId();

        final TestAuthenticationCredentials assigneeCaseWorkerWithIncorrectRoles =
            authorizationProvider
                .getNewWaTribunalCaseworkerWithStaticEmailAndStaticID("incorrectroletaskassignee.test");

        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Incorrect RoleAssignment Task Test"))
                .body("task.type", equalTo("incorrectRoleAssigneeTestTask"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.assignee", equalTo(null));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);


        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
        taskFunctionalTestsApiUtils.getCommon()
                .clearAllRoleAssignments(assigneeCaseWorkerWithIncorrectRoles.getHeaders());
        authorizationProvider.deleteAccount(assigneeCaseWorkerWithIncorrectRoles.getAccount().getUsername());
    }

    /*
     *This test covers the scenario where there are multiple assignees with correct roles for a task,
     *in which case the task should be assigned to the last assignee returned by the DMN configuration.

     *But as the DMN configuration is currently set up to return only one assignee, this test is ignored for now.
     *Once the code changes were made to allow multiple assignees to be returned by the DMN configuration,
     *this test should be enabled and the assertions should be updated to reflect the expected behaviour.
     */
    @Ignore
    @Test
    public void should_assign_task_to_last_assignee_when_multiple_assignees_with_correct_roles() {

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "multipleAssigneeTestTask", "Multiple Assignee Test Task"
            );
        String taskId = taskVariables.getTaskId();

        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Multiple Assignee Test Task"))
                .body("task.type", equalTo("multipleAssigneeTestTask"))
                .body("task.task_state", equalTo("assigned"))
                .body("task.assignee", equalTo("8b48c4bd-6281-32f8-a880-a3be29d3b952"));
        };


        TestAuthenticationCredentials multiAssigneeCaseWorker1 =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmailAndStaticID("multipletaskassignee.test1");

        final TestAuthenticationCredentials multiAssigneeCaseWorker2 =
            authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmailAndStaticID("multipletaskassignee.test2");

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccessWithAuthorizations(
            multiAssigneeCaseWorker1.getHeaders(),
            taskVariables.getCaseId(), TaskFunctionalTestConstants.WA_JURISDICTION,
            TaskFunctionalTestConstants.WA_CASE_TYPE);

        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccessWithAuthorizations(
            multiAssigneeCaseWorker2.getHeaders(),
            taskVariables.getCaseId(), TaskFunctionalTestConstants.WA_JURISDICTION,
            TaskFunctionalTestConstants.WA_CASE_TYPE);

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, multiAssigneeCaseWorker1.getHeaders(), assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "assigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(multiAssigneeCaseWorker1.getHeaders());
        authorizationProvider.deleteAccount(multiAssigneeCaseWorker1.getAccount().getUsername());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(multiAssigneeCaseWorker2.getHeaders());
        authorizationProvider.deleteAccount(multiAssigneeCaseWorker2.getAccount().getUsername());
    }

    @Test
    public void should_calculate_due_date_when_initiating_a_follow_up_overdue_respondent_evidence_using_due_date() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "followUpOverdueRespondentEvidence", "Follow-up overdue case building"
        );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Follow-up overdue case building"))
                .body("task.type", equalTo("followUpOverdueRespondentEvidence"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Follow-up overdue case building"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.description", equalTo("[Dummy Activity](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                  + "trigger/dummyActivity)"))
                .body("task.permissions.values.size()", equalTo(6))
                .body("task.permissions.values",
                      hasItems("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))).body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body(
                    "task.priority_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")))
                )
                .body("task.due_date", notNullValue())
                .body(
                    "task.due_date",
                    equalTo(OffsetDateTime.now(ZoneOffset.UTC)
                                .plusDays(2)
                                .withHour(18).withMinute(0).withSecond(0).withNano(0)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_calculate_due_date_when_initiating_a_multiple_calendar_task_using_due_date() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "multipleCalendarsDueDate", "Multiple calendars"
        );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Multiple calendars"))
                .body("task.type", equalTo("multipleCalendarsDueDate"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Multiple calendars"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("decision_making_work"))
                .body("task.work_type_label", equalTo("Decision-making work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Execute"))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body(
                    "task.priority_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")))
                )
                .body("task.due_date", notNullValue())
                .body("task.due_date", equalTo(LocalDateTime.of(2025, 12, 30, 18, 00, 0, 0)
                                                   .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_due_date_calculation_task_by_using_due_date_origin() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon()
                .setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
                                             "calculateDueDate", "Calculate Due Date");
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Calculate Due Date"))
                .body("task.type", equalTo("calculateDueDate"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Calculate Due Date"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.case_management_category", equalTo("Protection"))
                .body("task.description", equalTo(""))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Execute"))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body(
                    "task.priority_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")))
                )
                .body("task.due_date", notNullValue())
                .body(
                    "task.due_date",
                    equalTo(LocalDateTime.of(2025, 10, 23, 20, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_specific_access_task_by_id() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "reviewSpecificAccessRequestJudiciary",
                "additionalProperties_roleAssignmentId"
            );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("additionalProperties_roleAssignmentId"))
                .body("task.type", equalTo("reviewSpecificAccessRequestJudiciary"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("additionalProperties_roleAssignmentId"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("access_requests"))
                .body("task.work_type_label", equalTo("Access requests"))
                .body("task.role_category", equalTo("JUDICIAL"))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Own"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "roleAssignmentId", "roleAssignmentId")))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body("task.priority_date",
                      equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                  .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, caseWorkerWithJudgeRole.getHeaders(), assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
            TASK_GET_ROLES_ENDPOINT,
            taskId,
            caseWorkerWithJudgeRole.getHeaders()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("roles.values.size()", equalTo(9))
            .body("roles[0].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[0].role_name", equalTo("case-manager"))
            .body("roles[0].permissions", hasItems("Own", "Manage"))
            .body("roles[1].role_category", equalTo("ADMIN"))
            .body("roles[1].role_name", equalTo("challenged-access-admin"))
            .body("roles[1].permissions", hasItems("Own", "Manage"))
            .body("roles[2].role_category", equalTo("JUDICIAL"))
            .body("roles[2].role_name", equalTo("challenged-access-judiciary"))
            .body("roles[2].permissions", hasItems("Manage"))
            .body("roles[3].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[3].role_name", equalTo("challenged-access-legal-ops"))
            .body("roles[3].permissions", hasItems("Cancel"))
            .body("roles[4].role_category", equalTo("JUDICIAL"))
            .body("roles[4].role_name", equalTo("ftpa-judge"))
            .body("roles[4].permissions", hasItems("Execute", "Manage"))
            .body("roles[5].role_category", equalTo("JUDICIAL"))
            .body("roles[5].role_name", equalTo("hearing-panel-judge"))
            .body("roles[5].permissions", hasItems("Read", "Manage", "Cancel"))
            .body("roles[6].role_category", equalTo("JUDICIAL"))
            .body("roles[6].role_name", equalTo("judge"))
            .body("roles[6].permissions", hasItems("Read", "Own"))
            .body("roles[7].role_category", equalTo("JUDICIAL"))
            .body("roles[7].role_name", equalTo("lead-judge"))
            .body("roles[7].permissions", hasItems("Cancel"))
            .body("roles[8].role_name", equalTo("task-supervisor"))
            .body("roles[8].permissions", hasItems("Read", "Manage", "Cancel"));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_priority_date_when_initiating_a_task_without_hearing_date() {
        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_no_hearing_date.json",
            "processApplication",
            "process Application"
        );
        String taskId = taskVariables.getTaskId();

        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            // Commenting priority date and due date comparison as there is
            // business change based on new date calculation

            //ZonedDateTime dueDate = ZonedDateTime.parse(
            //    result.jsonPath().get("task.due_date"),
            //    ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
            //);
            //String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

            //ZonedDateTime priorityDate = ZonedDateTime.parse(
            //    result.jsonPath().get("task.priority_date"),
            //    ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
            //);
            //String formattedPriorityDate = CAMUNDA_DATA_TIME_FORMATTER.format(priorityDate);
            //Assert.assertEquals(formattedDueDate, formattedPriorityDate);

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("process Application"))
                .body("task.type", equalTo("processApplication"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("process Application"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.priority_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body(
                    "task.description",
                    equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                            + "trigger/decideAnApplication)")
                )
                .body("task.permissions.values.size()", equalTo(6))
                .body("task.permissions.values",
                      hasItems("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))).body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(5000));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_task_when_next_hearing_date_is_empty() {

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon()
                .setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_empty_hearing_date.json",
                                             "processApplication", "process application");
        String taskId = taskVariables.getTaskId();

        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("process application"))
                .body("task.type", equalTo("processApplication"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("process application"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.location", equalTo("765324"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_category", equalTo("Protection"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_management_category", equalTo("Protection"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body(
                    "task.permissions.values",
                    equalToObject(List.of("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))
                )
                .body(
                    "task.description",
                    equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                + "trigger/decideAnApplication)")
                )
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                )))
                .body("task.next_hearing_id", nullValue())
                .body("task.next_hearing_date", nullValue())
                .body("task.priority_date", not(""))
                .body("task.priority_date", notNullValue());
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_calculate_due_date_and_priority_date_using_intervals() {

        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon()
                .setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_empty_hearing_date.json",
                                             "functionalTestTask2", "functional Test Task 2");
        String taskId = taskVariables.getTaskId();

        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("functional Test Task 2"))
                .body("task.type", equalTo("functionalTestTask2"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("functional Test Task 2"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.location", equalTo("765324"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_category", equalTo("Protection"))
                .body("task.case_name", equalTo("Bob Smith"))
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_management_category", equalTo("Protection"))
                .body("task.next_hearing_date", nullValue())
                .body("task.priority_date", equalTo("2026-01-02T18:00:00+0000"))
                .body("task.due_date", equalTo("2026-01-02T18:00:00+0000"));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_initiate_review_appeal_skeleton_argument_task_with_ctsc_category() {
        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_no_hearing_date.json",
            "reviewAppealSkeletonArgument",
            "Review Appeal Skeleton Argument"
        );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Review Appeal Skeleton Argument"))
                .body("task.type", equalTo("reviewAppealSkeletonArgument"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Review Appeal Skeleton Argument"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("CTSC"))
                .body("task.description", equalTo("[Request respondent review](/case/WA/WaCaseType"
                                                      + "/${[CASE_REFERENCE]}/trigger/requestRespondentReview)<br />"
                                                      + "[Request case edit](/case/WA/WaCaseType/${[CASE_REFERENCE]}"
                                                      + "/trigger/requestCaseEdit)"))
                .body("task.permissions.values.size()", equalTo(3))
                .body("task.permissions.values", hasItems("Read", "Cancel", "Own"))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(5000));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, userWithCFTCtscRole.getHeaders(), assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
            TASK_GET_ROLES_ENDPOINT,
            taskId,
            userWithCFTCtscRole.getHeaders()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("roles.values.size()", equalTo(2))
            .body("roles[0].role_category", equalTo("CTSC"))
            .body("roles[0].role_name", equalTo("ctsc"))
            .body("roles[0].permissions", hasItems("Read", "Own", "Cancel"))
            .body("roles[1].role_name", equalTo("task-supervisor"))
            .body("roles[1].permissions", hasItems("Read", "Manage", "Cancel"));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_when_initiating_a_follow_up_overdue_task_by_id() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "followUpOverdue",
                "Follow Up Overdue"
            );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Follow Up Overdue"))
                .body("task.type", equalTo("followUpOverdue"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Follow Up Overdue"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.case_management_category", equalTo("Protection"))
                .body("task.description", equalTo(""))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Execute"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                )))
                .body("task.next_hearing_id", equalTo("next-hearing-id"))
                .body(
                    "task.next_hearing_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")))
                )
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body(
                    "task.priority_date",
                    equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, caseWorkerWithWAOrgRoles.getHeaders(), assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
            TASK_GET_ROLES_ENDPOINT,
            taskId,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("roles.values.size()", equalTo(3))
            .body("roles[0].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[0].role_name", equalTo("senior-tribunal-caseworker"))
            .body("roles[0].permissions", hasItems("Read", "Execute"))
            .body("roles[1].role_name", equalTo("task-supervisor"))
            .body("roles[1].permissions", hasItems("Read", "Manage", "Cancel"))
            .body("roles[2].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[2].role_name", equalTo("tribunal-caseworker"))
            .body("roles[2].permissions", hasItems("Read", "Execute"));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_503_if_task_already_initiated_however_handled_gracefully() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process Application"
        );

        String taskId = taskVariables.getTaskId();

        Consumer<Response> assertConsumer = result -> result.then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.name", equalTo("process Application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.task_system", equalTo("SELF"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.task_title", equalTo("process Application"))
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.auto_assigned", equalTo(false))
            .body("task.warnings", equalTo(false))
            .body("task.case_id", equalTo(taskVariables.getCaseId()))
            .body("task.case_type_id", equalTo("WaCaseType"))
            .body("task.case_category", equalTo("Protection"))
            .body("task.jurisdiction", equalTo("WA"))
            .body("task.region", equalTo("1"))
            .body("task.location", equalTo("765324"))
            .body("task.location_name", equalTo("Taylor House"))
            .body("task.execution_type", equalTo("Case Management Task"))
            .body("task.work_type_id", equalTo("hearing_work"))
            .body("task.work_type_label", equalTo("Hearing work"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.description",
                  equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                              + "trigger/decideAnApplication)"))
            .body("task.permissions.values.size()", equalTo(6))
            .body("task.permissions.values", hasItems("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))).body("task.minor_priority", equalTo(500))
            .body("task.major_priority", equalTo(1000));

        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, caseWorkerWithWAOrgRoles.getHeaders(), assertConsumer);
        //Expect to get 503 for database conflict
        taskFunctionalTestsInitiationUtils.initiateTask(
            taskVariables, caseWorkerWithWAOrgRoles.getHeaders(), assertConsumer);
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_502_if_task_is_missing_mandatory_task_attributes() {

        TestAuthenticationCredentials hearingPanelJudge =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "validateMandatoryTaskAttributesDuringInitiation",
            "validateMandatoryTaskAttributesDuringInitiation"
        );


        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            hearingPanelJudge.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );

        InitiateTaskRequestMap initiateTaskRequest = taskFunctionalTestsInitiationUtils.initiateTaskRequestMap(
            taskVariables, null);
        Response response = taskFunctionalTestsApiUtils.getRestApiActions().post(
            TASK_INITIATION_ENDPOINT,
            taskVariables.getTaskId(),
            initiateTaskRequest,
            authorizationProvider.getServiceAuthorizationHeadersOnly()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.BAD_GATEWAY.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("error", equalTo("Bad Gateway"))
            .body("status", equalTo(502))
            .body("message", containsString(MANDATORY_FIELD_MISSING_ERROR.getDetail()));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskVariables.getTaskId());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(hearingPanelJudge.getHeaders());
        authorizationProvider.deleteAccount(hearingPanelJudge.getAccount().getUsername());
    }

    @Test
    public void should_calculate_due_date_must_be_working_day_should_default_to_next() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "mustBeWorkingDayDefault", "Must be working day default"
        );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Must be working day default"))
                .body("task.type", equalTo("mustBeWorkingDayDefault"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Must be working day default"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("decision_making_work"))
                .body("task.work_type_label", equalTo("Decision-making work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Execute"))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body("task.priority_date", equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                                        .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))))
                .body("task.due_date", notNullValue())
                .body("task.due_date", equalTo(LocalDateTime.of(2025, 10, 15, 20, 00, 0, 0)
                                                   .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_calculate_due_date_multiple_working_day_should_use_last_entry_in_dmn() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data_fixed_hearing_date.json",
            "multipleMustBeWorkingDays", "Multiple must  be working day"
        );
        String taskId = taskVariables.getTaskId();

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Multiple must  be working day"))
                .body("task.type", equalTo("multipleMustBeWorkingDays"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Multiple must  be working day"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("decision_making_work"))
                .body("task.work_type_label", equalTo("Decision-making work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.permissions.values.size()", equalTo(2))
                .body("task.permissions.values", hasItems("Read", "Execute"))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body("task.priority_date", equalTo(LocalDateTime.of(2022, 12, 7, 14, 00, 0, 0)
                                                        .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))))
                .body("task.due_date", notNullValue())
                .body("task.due_date", equalTo(LocalDateTime.of(2025, 10, 15, 20, 00, 0, 0)
                                                   .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                                                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"))));
        };

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, assertConsumer);

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

}
