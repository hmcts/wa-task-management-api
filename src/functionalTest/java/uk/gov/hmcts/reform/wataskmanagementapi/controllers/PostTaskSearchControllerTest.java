package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task";

    private Headers authenticationHeaders;

    private static final Map<String, String> TASK_ID_WORK_TYPE_MAP = new HashMap<>() {{
        put("arrangeOfflinePayment", "routine_work");
        put("followUpOverdueReasonsForAppeal", "decision_making_work");
    }};

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        common.clearAllRoleAssignments(authenticationHeaders);
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
    public void given_sort_by_parameter_should_support_camelCase_and_snake_case() throws JsonProcessingException {
        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskAndRetrieveIds();
        TestVariables taskVariablesForTask2 = common.setupTaskAndRetrieveIds();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
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
            .containsSequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        // Given query
        searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
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
            .containsSequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        common.cleanUpTask(taskVariablesForTask1.getTaskId(), REASON_COMPLETED);
        common.cleanUpTask(taskVariablesForTask2.getTaskId(), REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_search_results() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_warnings() {
        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("total_records", greaterThanOrEqualTo(1))
            .body("tasks.warnings", everyItem(notNullValue()))
            .body("tasks.warning_list.values", everyItem(notNullValue()));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_all_tasks_without_pagination() {
        //creating 3 tasks
        String[] taskStates = {TaskState.ASSIGNED.value(), TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_limited_tasks_with_pagination() {
        //creating 3 tasks
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_firstResult_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.ASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_maxResults_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.UNASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_with_negative_pagination() {
        //creating 1 task
        String[] taskStates = {TaskState.UNCONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_unassigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_assigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response claimResult = restApiActions.post(
            "task/{task-id}/claim",
            taskId,
            authenticationHeaders
        );

        claimResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(STATE, SearchOperator.IN, singletonList("assigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("assigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId, REASON_COMPLETED);
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_empty_search_results_location_did_not_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "17595"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        common.cleanUpTask(taskId, REASON_COMPLETED);
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

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

        common.cleanUpTask(taskId, REASON_COMPLETED);
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId, REASON_COMPLETED);
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

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );


        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.id", hasItems(tasksCreated.get(0).getTaskId(), tasksCreated.get(1).getTaskId()))
            .body("tasks.case_id", hasItems(tasksCreated.get(0).getCaseId(), tasksCreated.get(1).getCaseId()))
            .body("tasks.task_state", everyItem(either(is("unassigned")).or(is("assigned"))))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(equalTo("IA")))
            .body("total_records", greaterThanOrEqualTo(1));


        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
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

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0))
            .body("total_records", equalTo(0));

        tasksCreated.forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));

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

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
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
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_work_type() {
        TestVariables taskVariables = common.setupTaskWithTaskIdAndRetrieveIds("followUpOverdueReasonsForAppeal");
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId))
            .body("total_records", greaterThanOrEqualTo(1))
            .body("tasks.warnings", everyItem(notNullValue()))
            .body("tasks.work_type", hasItem("decision_making_work"))
            .body("tasks.warning_list.values", everyItem(notNullValue()));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void given_search_by_work_type_should_return_only_one() throws JsonProcessingException {

        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskWithTaskIdAndRetrieveIds("followUpOverdueReasonsForAppeal");
        TestVariables taskVariablesForTask2 = common.setupTaskWithTaskIdAndRetrieveIds("arrangeOfflinePayment");
        String taskId1 = taskVariablesForTask1.getTaskId();
        String taskId2 = taskVariablesForTask2.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList(TASK_ID_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"))))
        );

        // When
        Response result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, authenticationHeaders);

        //Then
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariablesForTask1.getCaseId()))
            .body("tasks.id", hasItem(taskId1))
            .body("tasks.work_type", hasItem(TASK_ID_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal")))
            .body("total_records", equalTo(1));

        // Given query
        searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList(TASK_ID_WORK_TYPE_MAP.get("arrangeOfflinePayment"))))
        );

        // When
        result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, authenticationHeaders);

        //Then
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariablesForTask2.getCaseId()))
            .body("tasks.id", hasItem(taskId2))
            .body("tasks.work_type", hasItem(TASK_ID_WORK_TYPE_MAP.get("arrangeOfflinePayment")))
            .body("total_records", equalTo(1));


        common.cleanUpTask(taskVariablesForTask1.getTaskId(), REASON_COMPLETED);
        common.cleanUpTask(taskVariablesForTask2.getTaskId(), REASON_COMPLETED);
    }

    @Test
    public void given_search_by_multiple_work_type_should_return_all() throws JsonProcessingException {
        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskWithTaskIdAndRetrieveIds("followUpOverdueReasonsForAppeal");
        TestVariables taskVariablesForTask2 = common.setupTaskWithTaskIdAndRetrieveIds("arrangeOfflinePayment");
        String taskId1 = taskVariablesForTask1.getTaskId();
        String taskId2 = taskVariablesForTask2.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(
                new SearchParameter(WORK_TYPE, SearchOperator.IN, TASK_ID_WORK_TYPE_MAP.values()
                    .stream().collect(Collectors.toList()))
            )
        );

        // When
        Response result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, authenticationHeaders);

        //Then
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(50)) //Default max results
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItems(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId()))
            .body("tasks.id", hasItems(taskId1, taskId2))
            .body("tasks.work_type", hasItems(
                TASK_ID_WORK_TYPE_MAP.get("followUpOverdueReasonsForAppeal"),
                TASK_ID_WORK_TYPE_MAP.get("arrangeOfflinePayment")))
            .body("total_records", equalTo(2));

        common.cleanUpTask(taskVariablesForTask1.getTaskId(), REASON_COMPLETED);
        common.cleanUpTask(taskVariablesForTask2.getTaskId(), REASON_COMPLETED);
    }

    @Test
    public void given_sort_by_work_type_should_sort_by_work_type() throws JsonProcessingException {
        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskWithTaskIdAndRetrieveIds("arrangeOfflinePayment");
        TestVariables taskVariablesForTask2 = common.setupTaskWithTaskIdAndRetrieveIds("followUpOverdueReasonsForAppeal");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
            singletonList(new SortingParameter(SortField.WORK_TYPE_SNAKE_CASE, SortOrder.DESCENDANT))
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
            .containsSequence(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId());

        // Given query
        searchTaskRequest = new SearchTaskRequest(
            singletonList(new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))),
            singletonList(new SortingParameter(SortField.WORK_TYPE_CAMEL_CASE, SortOrder.ASCENDANT))
        );

        // When
        result = restApiActions.post(ENDPOINT_BEING_TESTED, searchTaskRequest, authenticationHeaders);

        // Then expect task2,task1 order
        actualCaseIdList = result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body().path("tasks.case_id");
        assertThat(actualCaseIdList).asList()
            .containsSequence(taskVariablesForTask2.getCaseId(), taskVariablesForTask1.getCaseId());

        common.cleanUpTask(taskVariablesForTask1.getTaskId(), REASON_COMPLETED);
        common.cleanUpTask(taskVariablesForTask2.getTaskId(), REASON_COMPLETED);
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

}

