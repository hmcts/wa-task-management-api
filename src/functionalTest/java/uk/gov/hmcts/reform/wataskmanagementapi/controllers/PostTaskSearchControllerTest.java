package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task";

    private Headers authenticationHeaders;

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
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));
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
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()))
            .body("tasks.id", hasItem(taskId));

        common.cleanUpTask(taskId, REASON_COMPLETED);
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
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("unassigned")));

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
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.task_state", everyItem(is("assigned")));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Ignore("https://tools.hmcts.net/jira/browse/RWA-411")
    @Test
    public void should_return_a_200_with_search_results_with_restricted_role_assignment() {
        TestVariables taskVariablesSscs =
            common.setupTaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition.JURISDICTION, "SSCS");
        String taskId = taskVariablesSscs.getTaskId();
        String caseId = taskVariablesSscs.getCaseId();

        common.setupTaskAndRetrieveIdsWithCustomVariable(CamundaVariableDefinition.JURISDICTION, "IA");


        common.setupRestrictedRoleAssignment(caseId, authenticationHeaders);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.jurisdiction", everyItem(is("SSCS")))
            .body("tasks.case_id", hasItem(caseId))
            .body("tasks.id", hasItem(taskId));

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
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()));

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
            .body("tasks.size()", equalTo(0));

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
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(taskVariables.getCaseId()));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_location_and_multiple_state_filters() {
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasksWithDifferentTaskStates(taskStates);

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
            .body("tasks.id", hasItems(tasksCreated.get(0).getTaskId(), tasksCreated.get(1).getTaskId()))
            .body("tasks.case_id", hasItems(tasksCreated.get(0).getCaseId(), tasksCreated.get(1).getCaseId()))
            .body("tasks.task_state", everyItem(either(is("unassigned")).or(is("assigned"))))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(equalTo("IA")));


        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_user_jurisdiction_permission_did_not_match() {

        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<TestVariables> tasksCreated = createMultipleTasksWithDifferentTaskStates(
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
            .body("tasks.size()", equalTo(0));

        tasksCreated.forEach(task -> common.cleanUpTask(task.getTaskId(), REASON_COMPLETED));

    }

    private List<TestVariables> createMultipleTasksWithDifferentTaskStates(String[] states) {
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

