package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task";

    @Test
    public void should_return_a_200_with_search_results() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        var taskId = task.get("taskId");

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(task.get("caseId")))
            .body("tasks.id", hasItem(taskId));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_and_location_filters() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324"
        );

        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        var taskId = task.get("taskId");

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(task.get("caseId")));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_empty_search_results_location_did_not_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "17595"
        );

        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        var taskId = task.get("taskId");

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
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

        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        var taskId = task.get("taskId");

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))

        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.id", hasItem(taskId))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_id", hasItem(task.get("caseId")));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_location_and_multiple_state_filters() {
        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<Map<String, String>> tasksCreated = createMultipleTasksWithDifferentTaskStates(taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameter(STATE, SearchOperator.IN, asList("unassigned", "assigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );


        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.id", hasItems(tasksCreated.get(0).get("taskId"), tasksCreated.get(1).get("taskId")))
            .body("tasks.case_id", hasItems(tasksCreated.get(0).get("caseId"), tasksCreated.get(1).get("caseId")))
            .body("tasks.task_state", everyItem(either(is("unassigned")).or(is("assigned"))))
            .body("tasks.location", everyItem(equalTo("765324")))
            .body("tasks.jurisdiction", everyItem(equalTo("IA")));


        tasksCreated.stream()
            .map(map -> map.get("taskId"))
            .forEach(taskId -> common.cleanUpTask(taskId, REASON_COMPLETED));
    }

    @Test
    public void should_return_a_200_with_empty_search_results_user_jurisdiction_permission_did_not_match() {

        String[] taskStates = {TaskState.UNASSIGNED.value(), TaskState.ASSIGNED.value(), TaskState.CONFIGURED.value()};

        List<Map<String, String>> tasksCreated = createMultipleTasksWithDifferentTaskStates(
            taskStates);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("17595", "17594")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchTaskRequest,
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", equalTo(0));

        tasksCreated.stream()
            .map(map -> map.get("taskId")).forEach(taskId -> common.cleanUpTask(taskId, REASON_COMPLETED));

    }


    private List<Map<String, String>> createMultipleTasksWithDifferentTaskStates(String[] states) {
        List<Map<String, String>> tasksCreated = new ArrayList<>();
        for (String state : states) {
            Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
                CamundaVariableDefinition.TASK_STATE, state
            );

            Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
            tasksCreated.add(task);
        }

        return tasksCreated;
    }

}

