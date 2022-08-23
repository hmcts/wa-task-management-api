package uk.gov.hmcts.reform.wataskmanagementapi.controllers.newinitiate;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestNew;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.HAS_WARNINGS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;

public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-mvp-ft-test-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_400_if_search_request_is_empty() {

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            new SearchTaskRequest(emptyList()),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() {

        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("total_records", equalTo(0))
            .body("tasks.size()", equalTo(0));
    }

    @Test
    public void given_sort_by_parameter_should_support_camelCase_and_snake_case() {
        // create some tasks
        TestVariables taskVariablesForTask1 = common.setupTaskAndRetrieveIds();
        TestVariables taskVariablesForTask2 = common.setupTaskAndRetrieveIds();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        // Given query
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(
            asList(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
                new SearchParameterList(
                    CASE_ID,
                    SearchOperator.IN,
                    asList(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId())
                )
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.DESCENDANT))
        );

        // When
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
                    asList(taskVariablesForTask1.getCaseId(), taskVariablesForTask2.getCaseId())
                )
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.DESCENDANT))
        );

        // When
        result = restApiActions
            .post(
                ENDPOINT_BEING_TESTED,
                searchTaskRequest,
                caseworkerCredentials.getHeaders()
            );

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

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_warnings() {
        TestVariables taskVariables = common.setupTaskWithWarningsAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
    public void should_return_a_200_with_all_tasks_without_pagination() {
        //creating 3 tasks
        String[] taskStates = {TaskState.ASSIGNED.value(), TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value()};

        List<TestVariables> tasksCreated = createMultipleTasks(taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2))
            .body("total_records", greaterThanOrEqualTo(1));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_400_with_empty_search_results_with_negative_firstResult_pagination() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=-1&max_results=2",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("violations[0].field", equalTo("search_with_criteria.first_result"))
            .body("violations[0].message", equalTo("first_result must not be less than zero"));

    }

    @Ignore("Release 1 tests")
    @Test
    public void should_return_a_400_with_empty_search_results_with_negative_maxResults_pagination() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=-1",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"))
            .body("title", equalTo("Constraint Violation"))
            .body("status", equalTo(HttpStatus.BAD_REQUEST.value()))
            .body("violations[0].field", equalTo("search_with_criteria.max_results"))
            .body("violations[0].message", equalTo("max_results must not be less than one"));

    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_unassigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=2",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(2))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")))
            .body("total_records", greaterThanOrEqualTo(1));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_state_assigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());
        initiateTask(taskVariables);

        Response claimResult = restApiActions.post(
            "task/{task-id}/claim",
            taskId,
            caseworkerCredentials.getHeaders()
        );

        claimResult.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("assigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            variablesOverride,
            "IA",
            "Asylum"
        );
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
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

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            variablesOverride,
            "IA",
            "Asylum"
        );
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            variablesOverride,
            "IA",
            "Asylum"
        );
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList(taskVariables.getCaseId())),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            variablesOverride,
            "IA",
            "Asylum"
        );
        String taskId = taskVariables.getTaskId();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("unassigned"))

        ));

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
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
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "17595",
                "jurisdiction", "IA"
            )
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
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
            new SearchParameterList(STATE, SearchOperator.IN, asList("unassigned", "assigned"))
        ));


        common.setupOrganisationalRoleAssignmentWithCustomAttributes(
            caseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "IA"
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

    private void initiateTask(TestVariables taskVariables) {

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequestNew req = new InitiateTaskRequestNew(
            INITIATION,
            Map.of(
                TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
                TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
                HAS_WARNINGS.value(), true,
                TITLE.value(), "A test task",
                CREATED.value(), formattedCreatedDate,
                CASE_ID.value(), taskVariables.getCaseId(),
                DUE_DATE.value(), formattedDueDate
            )
        );

        Response result = restApiActions.post(
            "task/{task-id}/new",
            taskVariables.getTaskId(),
            req,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }

    private List<TestVariables> createMultipleTasks(String[] states) {
        List<TestVariables> tasksCreated = new ArrayList<>();
        for (String state : states) {
            Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
                CamundaVariableDefinition.TASK_STATE, state
            );

            TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
                variablesOverride,
                "IA",
                "Asylum"
            );
            tasksCreated.add(taskVariables);
        }

        return tasksCreated;
    }

}
