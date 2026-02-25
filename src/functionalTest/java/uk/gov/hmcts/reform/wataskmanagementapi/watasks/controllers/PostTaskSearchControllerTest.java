package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.CASE_ID_CAMEL_CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_CFTC_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_GIN_INDEX;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_WA_ORG_ROLES2;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskSearchControllerTest {

    private static final String ENDPOINT_BEING_TESTED = "task";
    private static final String ASSIGNED_ENDPOINT = "task/{task-id}/assign";

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    TestAuthenticationCredentials caseWorkerWithWAOrgRoles;
    TestAuthenticationCredentials ginIndexCaseWorkerWithJudgeRole;
    TestAuthenticationCredentials hearingPanelJudgeForStandardAccess;
    TestAuthenticationCredentials userWithCFTCtscRole;

    @Before
    public void setUp() {
        caseWorkerWithWAOrgRoles = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_WA_ORG_ROLES2);
        ginIndexCaseWorkerWithJudgeRole = taskFunctionalTestsUserUtils.getTestUser(GIN_INDEX_CASE_WORKER_WITH_JUDGE_ROLE);
        hearingPanelJudgeForStandardAccess = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_JUDGE_ROLE_STD_ACCESS);
        userWithCFTCtscRole = taskFunctionalTestsUserUtils.getTestUser(CASE_WORKER_WITH_CFTC_ROLE);
    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties() {

        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json", "processApplication", "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json", "processApplication", "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID_CAMEL_CASE, SearchOperator.IN, caseIds),
                new SearchParameterList(TASK_TYPE, SearchOperator.IN, singletonList("processApplication"))
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.name", everyItem(equalTo("process application")))
            .body("tasks.type", everyItem(equalTo("processApplication")))
            .body("tasks.task_state", everyItem(equalTo("unassigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("process application")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))))
            .body("tasks.description", everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                         + "trigger/decideAnApplication)")))
            .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
            .body("tasks.next_hearing_id", everyItem(equalTo("next-hearing-id")))
            .body("tasks.next_hearing_date", everyItem(notNullValue()))
            .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))))
            .body("tasks.priority_date", everyItem(notNullValue()))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(1000)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties_for_granular_permission() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);
        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        ;
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();
        ;

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            hearingPanelJudgeForStandardAccess.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.name", everyItem(equalTo("process application")))
            .body("tasks.type", everyItem(equalTo("processApplication")))
            .body("tasks.task_state", everyItem(equalTo("unassigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("process application")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Manage", "Complete", "Assign", "Unassign"))))
            .body("tasks.description", everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                         + "trigger/decideAnApplication)")))
            .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
            .body("tasks.next_hearing_id", everyItem(equalTo("next-hearing-id")))
            .body("tasks.next_hearing_date", everyItem(notNullValue()))
            .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))))
            .body("tasks.priority_date", everyItem(notNullValue()))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(1000)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_and_additional_properties_and_granular_permission() {

        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId,
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.name", everyItem(equalTo("A Task")))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("access_requests")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Manage", "Claim"))));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_task_with_additional_properties_which_includes_in_configuration_dmn() {
        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId,
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        Map<String, String> expectedAdditionalProperties = Map.of("roleAssignmentId", roleAssignmentId);
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.additional_properties", everyItem(equalToObject(expectedAdditionalProperties)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_desc() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        OffsetDateTime hearingDate = OffsetDateTime.now();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps",hearingDate);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        OffsetDateTime hearingDate2 = OffsetDateTime.now().plusMinutes(10);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps",hearingDate2);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(1)))
            .body("tasks[1].id", is(taskIds.get(0)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_role_category_ctsc() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
            "reviewAppealSkeletonArgument",
            "Review Appeal Skeleton Argument");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, userWithCFTCtscRole.getHeaders());

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("CTSC")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            userWithCFTCtscRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.name", everyItem(equalTo("Review Appeal Skeleton Argument")))
            .body("tasks.type", everyItem(equalTo("reviewAppealSkeletonArgument")))
            .body("tasks.task_state", everyItem(equalTo("unassigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("Review Appeal Skeleton Argument")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Cancel"))))
            .body("tasks.description", everyItem(equalTo("[Request respondent review](/case/WA/WaCaseType"
                                                         + "/${[CASE_REFERENCE]}/trigger/requestRespondentReview)<br />"
                                                         + "[Request case edit](/case/WA/WaCaseType/${[CASE_REFERENCE]}"
                                                         + "/trigger/requestCaseEdit)")))
            .body("tasks.role_category", everyItem(equalTo("CTSC")))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(5000)))
            .body("total_records", equalTo(1));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_role_category_enforcement() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestAuthenticationCredentials caseworkerWithEnforcementRole =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            caseworkerWithEnforcementRole.getHeaders(), "enforcement");

        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
            "enforcementRoleCategoryTask", "Enforcement Role Category Task");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables,caseworkerWithEnforcementRole.getHeaders());

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("ENFORCEMENT")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerWithEnforcementRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.role_category", everyItem(equalTo("ENFORCEMENT")))
            .body("total_records", equalTo(1));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_asc() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        OffsetDateTime hearingDate = OffsetDateTime.now();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps",hearingDate);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        OffsetDateTime hearingDate2 = OffsetDateTime.now().plusMinutes(10);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps",hearingDate2);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(0)))
            .body("tasks[1].id", is(taskIds.get(1)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    //These tests are same as above tests but run through the gin index search

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties_using_search_index() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process application");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);
        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED
                + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseWorkerWithJudgeRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.name", everyItem(equalTo("process application")))
            .body("tasks.type", everyItem(equalTo("processApplication")))
            .body("tasks.task_state", everyItem(equalTo("unassigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("process application")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Manage", "Complete", "Assign", "Unassign"))))
            .body("tasks.description", everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                         + "trigger/decideAnApplication)")))
            .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
            .body("tasks.next_hearing_id", everyItem(equalTo("next-hearing-id")))
            .body("tasks.next_hearing_date", everyItem(notNullValue()))
            .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))))
            .body("tasks.priority_date", everyItem(notNullValue()))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(1000)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_for_my_work_and_correct_properties_using_search_index() {

        TestAuthenticationCredentials caseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        TestAuthenticationCredentials ginIndexCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
            "processApplication",
            "process application");
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(caseworkerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        taskFunctionalTestsApiUtils.getCommon().setupCaseManagerForSpecificAccess(ginIndexCaseworkerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        String assigneeId = taskFunctionalTestsUserUtils.getAssigneeId(ginIndexCaseworkerCredentials.getHeaders());
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ASSIGNED_ENDPOINT,
            taskId,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        String caseId = taskVariables.getCaseId();
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(USER, SearchOperator.IN, singletonList(assigneeId)),
                new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(caseId))
            )
        );

        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", everyItem(equalTo(taskId)))
            .body("tasks.name", everyItem(equalTo("process application")))
            .body("tasks.type", everyItem(equalTo("processApplication")))
            .body("tasks.task_state", everyItem(equalTo("assigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("process application")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", everyItem(equalTo(caseId)))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Manage",
                "CompleteOwn", "CancelOwn", "Claim"))))
            .body("tasks.description", everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                         + "trigger/decideAnApplication)")))
            .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
            .body("tasks.next_hearing_id", everyItem(equalTo("next-hearing-id")))
            .body("tasks.next_hearing_date", everyItem(notNullValue()))
            .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))))
            .body("tasks.priority_date", everyItem(notNullValue()))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(1000)))
            .body("total_records", equalTo(1));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());

        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_200_with_search_results_with_additional_properties_using_search_index() {

        String roleAssignmentId = UUID.randomUUID().toString();
        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId,
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data_fixed_hearing_date.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, additionalProperties);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        ;
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();
        ;

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseWorkerWithWAOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.name", everyItem(equalTo("A Task")))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("access_requests")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Manage", "Claim"))));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_desc_using_search_index() {
        TestAuthenticationCredentials ginIndexCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(ginIndexCaseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        OffsetDateTime hearingDate = OffsetDateTime.now();

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","processApplication", "process Application",hearingDate);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        OffsetDateTime hearingDate2 = OffsetDateTime.now().plusMinutes(10);

        taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","processApplication", "process Application",hearingDate2);
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(1)))
            .body("tasks[1].id", is(taskIds.get(0)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_200_with_role_category_ctsc_using_search_index() {
        TestAuthenticationCredentials ginIndexCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon().setupCFTCtscRoleAssignmentForWA(ginIndexCaseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
            "reviewAppealSkeletonArgument",
            "Review Appeal Skeleton Argument");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables, ginIndexCaseworkerCredentials.getHeaders());

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("CTSC")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.name", everyItem(equalTo("Review Appeal Skeleton Argument")))
            .body("tasks.type", everyItem(equalTo("reviewAppealSkeletonArgument")))
            .body("tasks.task_state", everyItem(equalTo("unassigned")))
            .body("tasks.task_system", everyItem(equalTo("SELF")))
            .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
            .body("tasks.task_title", everyItem(equalTo("Review Appeal Skeleton Argument")))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(equalTo("Taylor House")))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
            .body("tasks.jurisdiction", everyItem(equalTo("WA")))
            .body("tasks.region", everyItem(equalTo("1")))
            .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "Cancel"))))
            .body("tasks.description", everyItem(equalTo("[Request respondent review](/case/WA/WaCaseType"
                                                         + "/${[CASE_REFERENCE]}/trigger/requestRespondentReview)<br />"
                                                         + "[Request case edit](/case/WA/WaCaseType/${[CASE_REFERENCE]}"
                                                         + "/trigger/requestCaseEdit)")))
            .body("tasks.role_category", everyItem(equalTo("CTSC")))
            .body("tasks.minor_priority", everyItem(equalTo(500)))
            .body("tasks.major_priority", everyItem(equalTo(5000)))
            .body("total_records", equalTo(1));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_200_with_role_category_enforcement_using_search_index() {
        TestAuthenticationCredentials ginIndexCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            ginIndexCaseworkerCredentials.getHeaders(), "enforcement");

        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables
            = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
                                       "enforcementRoleCategoryTask", "Enforcement Role Category Task");
        tasksCreated.add(taskVariables);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables,ginIndexCaseworkerCredentials.getHeaders());

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("ENFORCEMENT")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
            .body("tasks.role_category", everyItem(equalTo("ENFORCEMENT")))
            .body("total_records", equalTo(1));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }


    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_asc_using_search_index() {
        TestAuthenticationCredentials ginIndexCaseworkerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_GIN_INDEX);
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(ginIndexCaseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        OffsetDateTime hearingDate = OffsetDateTime.now();

        TestVariables taskVariables1 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","followUpOverdueRespondentEvidence", "follow Up Overdue Respondent Evidence",hearingDate);
        tasksCreated.add(taskVariables1);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables1);

        OffsetDateTime hearingDate2 = OffsetDateTime.now().plusMinutes(10);

        TestVariables taskVariables2 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIdsWithCustomHearingDate("requests/ccd/wa_case_data.json","followUpOverdueRespondentEvidence", "follow Up Overdue Respondent Evidence",hearingDate2);
        tasksCreated.add(taskVariables2);
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables2);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).toList();
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).toList();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            ginIndexCaseworkerCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(0)))
            .body("tasks[1].id", is(taskIds.get(1)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> taskFunctionalTestsApiUtils.getCommon().cleanUpTask(task.getTaskId()));
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(ginIndexCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(ginIndexCaseworkerCredentials.getAccount().getUsername());
    }


}
