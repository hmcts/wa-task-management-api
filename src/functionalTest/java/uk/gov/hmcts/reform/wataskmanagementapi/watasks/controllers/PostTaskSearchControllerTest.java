package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task";

    private TestAuthenticationCredentials caseworkerCredentials;
    private TestAuthenticationCredentials granularPermissionCaseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        granularPermissionCaseworkerCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        common.clearAllRoleAssignments(granularPermissionCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(granularPermissionCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties() {
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication", "process application");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json", "processApplication", "process application");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID_CAMEL_CASE, SearchOperator.IN, caseIds),
                new SearchParameterList(TASK_TYPE, SearchOperator.IN, singletonList("processApplication"))
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own"))))
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
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties_for_granular_permission() {
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
            "processApplication",
            "process application");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                         "processApplication",
                                                         "process application");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        common.setupHearingPanelJudgeForStandardAccess(granularPermissionCaseworkerCredentials.getHeaders(),
            "WA",
            "WaCaseType"
        );

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            granularPermissionCaseworkerCredentials.getHeaders()
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
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Manage"))))
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
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties_and_granular_permission() {

        common.setupWAOrganisationalRoleAssignment(granularPermissionCaseworkerCredentials.getHeaders());
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

        TestVariables taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables, additionalProperties);

        taskVariables = common.setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties, "requests/ccd/wa_case_data.json", "reviewSpecificAccessRequestLegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables, additionalProperties);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            granularPermissionCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.name", everyItem(equalTo("process application")))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Own", "CompleteOwn", "CancelOwn", "Claim"))));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_task_with_additional_properties_which_includes_in_configuration_dmn() {
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
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

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "Review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables, additionalProperties);

        taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "Review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables, additionalProperties);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_desc() {
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            RequestContext.ALL_WORK,
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(1)))
            .body("tasks[1].id", is(taskIds.get(0)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_role_category_ctsc() {
        common.setupCFTCtscRoleAssignmentForWA(caseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables
            = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
            "reviewAppealSkeletonArgument",
            "Review Appeal Skeleton Argument");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables, caseworkerCredentials.getHeaders());

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("CTSC")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }


    @Test
    public void should_return_200_with_tasks_sorted_on_next_hearing_date_asc() {
        common.setupWAOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        taskVariables = common.setupWATaskAndRetrieveIds("reviewSpecificAccessRequestLegalOps", "review Specific Access Request LegalOps");
        tasksCreated.add(taskVariables);
        initiateTask(taskVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
            ),
            List.of(new SortingParameter(SortField.NEXT_HEARING_DATE_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks[0].id", is(taskIds.get(0)))
            .body("tasks[1].id", is(taskIds.get(1)))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

}
