package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskSearchControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task";
    private static final String TASK_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String CAMUNDA_SEARCH_HISTORY_ENDPOINT = "/history/variable-instance";
    private static final Map<String, String> TASK_TYPE_WORK_TYPE_MAP = new HashMap<>() {
        {
            put("arrangeOfflinePayment", "routine_work");
            put("followUpOverdueReasonsForAppeal", "decision_making_work");
        }
    };

    private Headers headers;

    @Before
    public void setUp() {
        headers = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
    }

    @Test
    public void should_return_a_400_if_search_request_is_empty() {

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            new SearchTaskRequest(emptyList()),
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_a_200_empty_list_when_the_user_did_not_have_any_roles() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
    }

    @Test
    public void given_sort_by_parameter_should_support_camelCase_and_snake_case() {
        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskAndRetrieveIds();
        TestVariables taskVariablesForTask2 = common.setupTaskAndRetrieveIds();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariablesForTask1, "followUpOverdueReasonsForAppeal", headers);
        common.insertTaskInCftTaskDb(taskVariablesForTask2, "followUpOverdueReasonsForAppeal", headers);

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
                new SearchParameterList(
                    CASE_ID,
                    SearchOperator.IN,
                    asList(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId()))
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        // When
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        // Then expect task2,tak1 order
        List<String> actualCaseIdList = result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body().path("tasks.case_id");

        assertThat(actualCaseIdList).asList()
            .containsSubsequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        // Given query
        searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
                new SearchParameterList(
                    CASE_ID,
                    SearchOperator.IN,
                    asList(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId()))
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        // When
        result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, headers);

        // Then expect task2,task1 order
        actualCaseIdList = result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body().path("tasks.case_id");
        assertThat(actualCaseIdList).asList()
            .containsSubsequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        common.cleanUpTask(taskVariablesForTask1.getTaskId(), taskVariablesForTask2.getTaskId());
    }

    @Test
    public void should_return_a_200_with_search_results_and_warnings() {
        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        final String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("total_records", greaterThanOrEqualTo(1))
            .body("tasks.warnings", everyItem(notNullValue()))
            .body("tasks.warning_list.values", everyItem(notNullValue()));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_limited_tasks_with_pagination() {
        //creating 3 tasks
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(2))
            .body("total_records", greaterThanOrEqualTo(1));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_firstResult_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.ASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=-1&max_results=2",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_maxResults_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.UNASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=-1",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.UNCONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=-1&max_results=-1",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_unassigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        final String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_assigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        final String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("assigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("assigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_and_location_filters() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        final String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_when_role_assignment_has_no_end_date_time() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        final String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
            ));

        common.setupOrganisationalRoleAssignmentWithOutEndDate(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].id", equalTo(taskId))
            .body("tasks[0].location", equalTo("765324"))
            .body("tasks[0].jurisdiction", equalTo("IA"))
            .body("total_records", equalTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_empty_search_results_location_did_not_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "17595"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        final String taskId = taskVariables.getTaskId();

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        common.setupCFTOrganisationalRoleAssignment(headers);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_caseId_location_filters() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_STATE, "unassigned"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId())),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks[0].permissions.values", hasItems("Read","Refer","Own","Manage","Cancel"))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    /**
     *  This scenario will test with default pagination i.e 50
     *
     */
    @Test
    public void should_return_a_200_with_default_search_results_based_on_jurisdiction_location_and_state() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_STATE, "unassigned"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        final String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_location_and_multiple_state_filters() {
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(STATE, SearchOperator.IN, asList("unassigned", "assigned"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );


        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.task_state", everyItem(either(is("unassigned")).or(is("assigned"))))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(equalTo("IA")))
            .body("total_records", greaterThanOrEqualTo(1));


        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_user_jurisdiction_permission_did_not_match() {

        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(
            taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("SSCS")),
            new SearchParameterList(LOCATION, SearchOperator.IN, asList("17595", "17594")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "17595",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));


        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        tasksCreated.forEach(task -> common.cleanUpTask(task.getTaskId()));

    }

    @Test
    public void should_return_a_200_with_search_results_and_correct_properties() {
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(STATE, SearchOperator.IN, asList("unassigned", "assigned")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, false)
        ));


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            headers,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "followUpOverdueReasonsForAppeal", headers));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.name", everyItem(notNullValue()))
            .body("tasks.type", everyItem(notNullValue()))
            .body("tasks.task_state", everyItem(notNullValue()))
            .body("tasks.task_system", everyItem(notNullValue()))
            .body("tasks.security_classification", everyItem(notNullValue()))
            .body("tasks.task_title", everyItem(notNullValue()))
            .body("tasks.created_date", everyItem(notNullValue()))
            .body("tasks.due_date", everyItem(notNullValue()))
            .body("tasks.location_name", everyItem(notNullValue()))
            .body("tasks.location", everyItem(notNullValue()))
            .body("tasks.execution_type", everyItem(notNullValue()))
            .body("tasks.jurisdiction", everyItem(notNullValue()))
            .body("tasks.region", everyItem(notNullValue()))
            .body("tasks.case_type_id", everyItem(notNullValue()))
            .body("tasks.case_id", everyItem(notNullValue()))
            .body("tasks.case_type_id", everyItem(notNullValue()))
            .body("tasks.case_category", everyItem(notNullValue()))
            .body("tasks.case_name", everyItem(notNullValue()))
            .body("tasks.auto_assigned", everyItem(notNullValue()))
            .body("tasks.warnings", everyItem(notNullValue()))
            .body("tasks.description", everyItem(notNullValue()))
            .body("tasks.permissions.values", everyItem(notNullValue()))
            .body("total_records", greaterThanOrEqualTo(1));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_have_consistent_unassigned_state_in_camunda_and_cft_db() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks[0].id", equalTo(taskId))
            .body("tasks[0].task_state", equalTo("unassigned"));

        Response camundaTask = camundaApiActions.get(
            "/task/{task-id}/variables",
            taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        camundaTask.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("taskState.value", is("unassigned"))
            .body("cftTaskState.value", is("unassigned"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_200_status_with_task_description_matching_to_dmn_description_value() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "decideOnTimeExtension", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks[0].id", equalTo(taskId))
            .body("tasks[0].task_state", equalTo("unassigned"))
            .body("tasks[0].description",  equalTo(
                "[Change the direction due date](/case/IA/Asylum/${[CASE_REFERENCE]}/trigger/changeDirectionDueDate)"
            ));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_200_status_with_empty_task_description_when_dmn_description_value_not_exists() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueCaseBuilding", headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks[0].id", equalTo(taskId))
            .body("tasks[0].task_state", equalTo("unassigned"))
            .body("tasks[0].description", equalTo(""));

        common.cleanUpTask(taskId);
    }

    /**
     * Terminate task with state CANCELLED will remove cftTaskState from Camunda history table.
     */
    @Test
    public void should_have_consistent_cancelled_state() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        // insert task in CftTaskDb
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        // verify cftTaskState exists in Camunda history table before termination
        cftTaskStateVariableShouldExistInCamundaHistoryTable(taskId);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("cancelled")
        );

        // Terminate task will remove record from camunda history table.
        Response response = restApiActions.delete(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            terminateTaskRequest,
            headers
        );
        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // verify cftTaskState does not exist in Camunda history table before termination
        cftTaskStateVariableShouldNotExistInCamundaHistoryTable(taskId);

        common.cleanUpTask(taskId);
    }

    /**
     * Terminate task with state COMPLETED will remove cftTaskState from Camunda history table.
     */
    @Test
    public void should_have_consistent_completed_state() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        // insert task in CftTaskDb
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", headers);

        // verify cftTaskState exists in Camunda history table before termination
        cftTaskStateVariableShouldExistInCamundaHistoryTable(taskId);

        TerminateTaskRequest terminateTaskRequest = new TerminateTaskRequest(
            new TerminateInfo("completed")
        );

        // Terminate task.
        Response response = restApiActions.delete(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            terminateTaskRequest,
            headers
        );
        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // verify cftTaskState does not exist in Camunda history table before termination
        cftTaskStateVariableShouldNotExistInCamundaHistoryTable(taskId);

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_work_type_and_return_a_task_with_same_work_type() {
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN,
                singletonList(TASK_TYPE_WORK_TYPE_MAP.get(taskType))),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("tasks.work_type_id", everyItem(is(TASK_TYPE_WORK_TYPE_MAP.get(taskType))))
            .body("total_records", equalTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_multiple_work_types_and_return_tasks_for_each_work_type() {
        //initiate first task
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        final String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers, "task-supervisor");

        common.insertTaskInCftTaskDb(taskVariables1, taskType, headers);

        //initiate second task
        taskType = "arrangeOfflinePayment";
        TestVariables taskVariables2 = common.setupTaskAndRetrieveIds(taskType);
        String taskId2 = taskVariables2.getTaskId();

        common.insertTaskInCftTaskDb(taskVariables2, taskType, headers);

        //search by all work types
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN,
                TASK_TYPE_WORK_TYPE_MAP.values().stream().collect(Collectors.toList())),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                asList(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(2))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", contains(taskVariables2.getCaseId(), taskVariables1.getCaseId()))
            .body("tasks.id", contains(taskId2, taskId1))
            .body("tasks.work_type_id", hasItems(
                TASK_TYPE_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"),
                TASK_TYPE_WORK_TYPE_MAP.get("arrangeOfflinePayment"))
            )
            .body("total_records", equalTo(2));

        common.cleanUpTask(taskId1, taskId2);
    }

    @Test
    public void should_return_400_when_search_by_invalid_work_type() {
        //initiate first task
        String taskType = "reviewTheAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables1, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("aWorkType"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        common.cleanUpTask(taskVariables1.getTaskId());
    }

    @Test
    public void should_return_empty_list_when_search_by_work_type_exists_and_case_id_not_exists() {
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        final String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN,
                singletonList(TASK_TYPE_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"))),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("dummyCaseId"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_case_ids_and_multiple_work_types_and_return_tasks_for_each_work_type() {
        //initiate first task
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        final String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers,"task-supervisor");

        common.insertTaskInCftTaskDb(taskVariables1, taskType, headers);

        //initiate second task
        taskType = "arrangeOfflinePayment";
        TestVariables taskVariables2 = common.setupTaskAndRetrieveIds(taskType);
        String taskId2 = taskVariables2.getTaskId();

        common.insertTaskInCftTaskDb(taskVariables2, taskType, headers);

        //search by all work types and caseIds
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN,
                TASK_TYPE_WORK_TYPE_MAP.values().stream().collect(Collectors.toList())),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                asList(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(2))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItems(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
            .body("tasks.id", hasItems(taskId1, taskId2))
            .body("tasks.work_type_id", hasItems(
                TASK_TYPE_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"),
                TASK_TYPE_WORK_TYPE_MAP.get("arrangeOfflinePayment"))
            )
            .body("total_records", equalTo(2));

        common.cleanUpTask(taskId1, taskId2);
    }

    @Test
    public void should_search_by_role_category_legal_operations_and_return_tasks_with_role_category_as_legal_operations() {
        String taskType = "processApplication";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN,
                singletonList("LEGAL_OPERATIONS")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("tasks.role_category", everyItem(is("LEGAL_OPERATIONS")))
            .body("total_records", equalTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_role_category_administrator_and_return_tasks_with_role_category_as_administrator() {
        String taskType = "arrangeOfflinePayment";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN,
                singletonList("ADMINISTRATOR")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("tasks.role_category", everyItem(is("ADMINISTRATOR")))
            .body("total_records", equalTo(1));


        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_role_category_judicial_and_return_tasks_with_role_category_as_judiciary() {
        String taskType = "reviewAddendumHomeOfficeEvidence";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers);

        common.insertTaskInCftTaskDb(taskVariables, taskType, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN,
                singletonList("JUDICIAL")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("tasks.role_category", everyItem(is("JUDICIAL")))
            .body("total_records", equalTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_search_by_any_role_category_and_return_tasks_with_appropriate_role_category() {
        //initiate first task
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        final String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(headers,"task-supervisor");

        common.insertTaskInCftTaskDb(taskVariables1, taskType, headers);

        //initiate second task
        taskType = "arrangeOfflinePayment";
        TestVariables taskVariables2 = common.setupTaskAndRetrieveIds(taskType);

        String taskId2 = taskVariables2.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables2, taskType, headers);

        //search by all work types and caseIds
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN,
                List.of("LEGAL_OPERATIONS", "ADMINISTRATOR")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                asList(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(2))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItems(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
            .body("tasks.id", hasItems(taskId1, taskId2))
            .body("tasks.role_category", hasItems("LEGAL_OPERATIONS", "ADMINISTRATOR"))
            .body("total_records", equalTo(2));

        common.cleanUpTask(taskId1);
    }

    @Test
    public void should_return_a_200_with_single_task_in_search_results_when_available_tasks_only_is_set_true() {

        String taskType1 = "reviewHearingBundle";
        String taskType2 = "reviewAdditionalAppellantEvidence";

        String caseId = given.iCreateACcdCase();
        List<CamundaTask>  camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType1);
        String taskId1 = camundaTasks.get(0).getId();

        camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType2);
        String taskId2 = camundaTasks.get(0).getId();

        common.setupCFTOrganisationalWithMultipleRoles(headers);

        // insert taskId1
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId1, "processInstanceId1"),
            taskType1, headers);

        // insert taskId2
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId2, "processInstanceId1"),
            taskType2, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(caseId)),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", is(1)) //Default max results
            .body("tasks[0].id", equalTo(taskId2))
            .body("tasks[0].permissions.values", hasItem("Own"))
            .body("total_records", is(1));

        common.cleanUpTask(taskId1, taskId2);
    }

    @Test
    public void should_return_a_200_with_multiple_tasks_in_search_results_when_available_tasks_only_is_set_true() {

        String taskType1 = "reviewAdditionalHomeOfficeEvidence";
        String taskType2 = "reviewAdditionalAppellantEvidence";

        String caseId = given.iCreateACcdCase();
        List<CamundaTask>  camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType1);
        String taskId1 = camundaTasks.get(0).getId();

        camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType2);
        String taskId2 = camundaTasks.get(0).getId();

        common.setupCFTOrganisationalWithMultipleRoles(headers);

        // insert taskId1
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId1, "processInstanceId1"),
            taskType1, headers);

        // insert taskId2
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId2, "processInstanceId1"),
            taskType2, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(caseId)),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", is(2)) //Default max results
            .body("tasks.id", hasItems(taskId1, taskId2))
            .body("tasks.permissions.values", everyItem(hasItem("Own")))
            .body("total_records", is(2));

        common.cleanUpTask(taskId1, taskId2);
    }

    @Test
    public void should_return_a_200_with_empty_tasks_in_search_results_when_available_tasks_only_is_set_true() {

        String taskType1 = "reviewAdditionalHomeOfficeEvidence";
        String taskType2 = "reviewAdditionalAppellantEvidence";

        String caseId = given.iCreateACcdCase();
        List<CamundaTask>  camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType1);
        String taskId1 = camundaTasks.get(0).getId();
        camundaTasks = common.setupTaskAndRetrieveIdsForGivenCaseId(caseId, taskType2);

        String taskId2 = camundaTasks.get(0).getId();

        common.setupCFTOrganisationalRoleAssignment(headers,"task-supervisor");

        // insert taskId1
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId1, "processInstanceId1"),
            taskType1, headers);

        // insert taskId2
        common.insertTaskInCftTaskDb(new TestVariables(caseId, taskId2, "processInstanceId1"),
            taskType2, headers);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(CASE_ID, SearchOperator.IN,
                singletonList(caseId)),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            headers
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        common.cleanUpTask(taskId1, taskId2);
    }

    private List<TestVariables> createMultipleTasks(String[] states) {
        List<TestVariables> tasksCreated = new ArrayList<>();
        for (String state : states) {
            Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
                CamundaVariableDefinition.TASK_STATE, state
            );

            TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
            tasksCreated.add(taskVariables);
        }

        return tasksCreated;
    }

    private void cftTaskStateVariableShouldExistInCamundaHistoryTable(String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );
        Response camundaHistoryResponse = camundaApiActions.post(CAMUNDA_SEARCH_HISTORY_ENDPOINT, body,
            authorizationHeadersProvider.getServiceAuthorizationHeadersOnly());
        camundaHistoryResponse.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("size()", is(1))
            .body("name", Matchers.hasItem("cftTaskState"));
    }

    private void cftTaskStateVariableShouldNotExistInCamundaHistoryTable(String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );
        Response camundaHistoryResponse = camundaApiActions.post(CAMUNDA_SEARCH_HISTORY_ENDPOINT, body,
            authorizationHeadersProvider.getServiceAuthorizationHeadersOnly());
        camundaHistoryResponse.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("size()", is(0));
    }
}
