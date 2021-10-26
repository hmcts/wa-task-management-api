package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TerminateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.time.ZonedDateTime;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.WORK_TYPE;

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

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
    }

    @Test
    public void should_return_a_400_if_search_request_is_empty() {

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            new SearchTaskRequest(emptyList()),
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_a_200_empty_list_when_the_user_did_not_have_any_roles() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            authenticationHeaders
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

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariablesForTask1.getCaseId(), taskVariablesForTask1.getTaskId());
        insertTaskInCftTaskDb(taskVariablesForTask2.getCaseId(), taskVariablesForTask2.getTaskId());

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
                new SearchParameter(
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
            authenticationHeaders
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
                new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
                new SearchParameter(
                    CASE_ID,
                    SearchOperator.IN,
                    asList(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId()))
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        // When
        result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, authenticationHeaders);

        // Then expect task2,task1 order
        actualCaseIdList = result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body().path("tasks.case_id");
        assertThat(actualCaseIdList).asList()
            .containsSubsequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        common.cleanUpTask(taskVariablesForTask1.getTaskId());
        common.cleanUpTask(taskVariablesForTask2.getTaskId());
    }

    @Test
    public void should_return_a_200_with_search_results() {

        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_warnings() {
        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            authenticationHeaders
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
    public void should_return_a_200_with_max_50_tasks_without_pagination_parameter() {
        //creating 3 tasks
        String[] taskStates = {TaskState.ASSIGNED.value(), TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", greaterThanOrEqualTo(3))
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("total_records", greaterThanOrEqualTo(1));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_return_a_200_with_limited_tasks_with_pagination() {
        //creating 3 tasks
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
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

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=-1&max_results=2",
            searchTaskRequest,
            authenticationHeaders
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

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=-1",
            searchTaskRequest,
            authenticationHeaders
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

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=-1&max_results=-1",
            searchTaskRequest,
            authenticationHeaders
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
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_assigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, singletonList("assigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            authenticationHeaders
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
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
            .body("tasks.id", hasItem(taskId))
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
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324"))),
            singletonList(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT)
            ));

        common.setupOrganisationalRoleAssignmentWithOutEndDate(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_empty_search_results_location_did_not_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "17595"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
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
            new SearchParameter(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId())),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_location_and_state_filters() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_STATE, "unassigned"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))

        ));

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
            .body("tasks.id", hasItem(taskId))
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, asList("unassigned", "assigned"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=3",
            searchTaskRequest,
            authenticationHeaders
        );


        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(3)) //Default max results
            .body("tasks.id", hasItems(tasksCreated.get(0).getTaskId(), tasksCreated.get(1).getTaskId()))
            .body("tasks.case_id", hasItems(tasksCreated.get(0).getCaseId(), tasksCreated.get(1).getCaseId()))
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("17595", "17594")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "17595",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, asList("unassigned", "assigned"))
        ));


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            authenticationHeaders,
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        tasksCreated.forEach(task -> insertTaskInCftTaskDb(task.getCaseId(), task.getTaskId()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2)) //Default max results
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
            .body("total_records", greaterThanOrEqualTo(1));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Test
    public void should_have_consistent_unassigned_state_in_camunda_and_cft_db() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
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

    /**
     * Terminate task with state CANCELLED will remove cftTaskState from Camunda history table.
     */
    @Test
    public void should_have_consistent_cancelled_state() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        // insert task in CftTaskDb
        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

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
            authenticationHeaders
        );
        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // verify cftTaskState does not exist in Camunda history table before termination
        cftTaskStateVariableShouldNotExistInCamundaHistoryTable(taskId);

        cleanUp(taskId);
    }

    /**
     * Terminate task with state COMPLETED will remove cftTaskState from Camunda history table.
     */
    @Test
    public void should_have_consistent_completed_state() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        // insert task in CftTaskDb
        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId);

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
            authenticationHeaders
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

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId, taskType);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(WORK_TYPE, SearchOperator.IN,
                singletonList(TASK_TYPE_WORK_TYPE_MAP.get(taskType))),
            new SearchParameter(CASE_ID, SearchOperator.IN,
                singletonList(taskVariables.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(1))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("tasks.work_type_id", everyItem(is(TASK_TYPE_WORK_TYPE_MAP.get(taskType))))
            .body("total_records", equalTo(1));
    }

    @Test
    public void should_search_by_multiple_work_types_and_return_tasks_for_each_work_type() {
        //initiate first task
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders, "task-supervisor");

        insertTaskInCftTaskDb(taskVariables1.getCaseId(), taskId1, taskType);

        //initiate second task
        taskType = "arrangeOfflinePayment";
        TestVariables taskVariables2 = common.setupTaskAndRetrieveIds(taskType);
        String taskId2 = taskVariables2.getTaskId();

        insertTaskInCftTaskDb(taskVariables2.getCaseId(), taskId2, taskType);

        //search by all work types
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(WORK_TYPE, SearchOperator.IN,
                TASK_TYPE_WORK_TYPE_MAP.values().stream().collect(Collectors.toList())),
            new SearchParameter(CASE_ID, SearchOperator.IN,
                asList(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
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
    }

    @Test
    public void should_return_400_when_search_by_invalid_work_type() {
        //initiate first task
        String taskType = "reviewTheAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables1.getCaseId(), taskId1, taskType);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("aWorkType"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_empty_list_when_search_by_work_type_exists_and_case_id_not_exists() {
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables = common.setupTaskAndRetrieveIds(taskType);
        String taskId = taskVariables.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskId, taskType);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(WORK_TYPE, SearchOperator.IN,
                singletonList(TASK_TYPE_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"))),
            new SearchParameter(CASE_ID, SearchOperator.IN, singletonList("dummyCaseId"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
    }

    @Test
    public void should_search_by_case_ids_and_multiple_work_types_and_return_tasks_for_each_work_type() {
        //initiate first task
        String taskType = "followUpOverdueReasonsForAppeal";
        TestVariables taskVariables1 = common.setupTaskAndRetrieveIds(taskType);
        String taskId1 = taskVariables1.getTaskId();

        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders,"task-supervisor");

        insertTaskInCftTaskDb(taskVariables1.getCaseId(), taskId1, taskType);

        //initiate second task
        taskType = "arrangeOfflinePayment";
        TestVariables taskVariables2 = common.setupTaskAndRetrieveIds(taskType);
        String taskId2 = taskVariables2.getTaskId();

        insertTaskInCftTaskDb(taskVariables2.getCaseId(), taskId2, taskType);

        //search by all work types and caseIds
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(WORK_TYPE, SearchOperator.IN,
                TASK_TYPE_WORK_TYPE_MAP.values().stream().collect(Collectors.toList())),
            new SearchParameter(CASE_ID, SearchOperator.IN,
                asList(taskVariables1.getCaseId(), taskVariables2.getCaseId()))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
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

    private void insertTaskInCftTaskDb(String caseId, String taskId) {
        String warnings = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                          + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "followUpOverdueReasonsForAppeal"),
            new TaskAttribute(TASK_NAME, "follow Up Overdue Reasons For Appeal"),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CASE_CATEGORY, "Protection"),
            new TaskAttribute(TASK_ROLE_CATEGORY, "LEGAL_OPERATIONS"),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_WARNINGS, warnings),
            new TaskAttribute(TASK_AUTO_ASSIGNED, true),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }

    private void insertTaskInCftTaskDb(String caseId, String taskId, String taskType) {
        String warnings = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                          + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";


        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CASE_CATEGORY, "Protection"),
            new TaskAttribute(TASK_ROLE_CATEGORY, "LEGAL_OPERATIONS"),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_WARNINGS, warnings),
            new TaskAttribute(TASK_AUTO_ASSIGNED, true),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            TASK_ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
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
