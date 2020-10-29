package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.STATE;

public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {


    @Value("${targets.instance}")
    private String testUrl;


    @Autowired
    private AuthorizationHeadersProvider authorizationHeadersProvider;

    @Test
    public void should_return_a_400_if_search_request_is_empty() {

        Response result = restApiActions.post(
            "task",
            new SearchTaskRequest(emptyList()),
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_a_200_with_search_results() {

        Map<String, String> task1 = common.setupTaskAndRetrieveIds();
        Map<String, String> task2 = common.setupTaskAndRetrieveIds();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));

        Response result = restApiActions.post(
            "task",
            searchTaskRequest,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.case_data.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_data.reference", hasItems(task1.get("ccdId"), task2.get("ccdId")));
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_and_location_filters() {

        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "IA")
            .withProcessVariable("location", "17595")
            .withProcessVariable("locationName", "A Hearing Centre")
            .withProcessVariable("taskState", "assigned")
            .build();

        Map<String, String> task = setUpTaskWithCustomVariables(processVariables);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, singletonList("17595"))
        ));

        Response result = restApiActions.post(
            "task",
            searchTaskRequest,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.state", everyItem(either(is("unassigned")).or(is("assigned"))))
            .body("tasks.case_data.reference", hasItem(task.get("ccdId")))
            .body("tasks.case_data.location.id", everyItem(equalTo("17595")));
    }

    @Test
    public void should_return_a_200_with_search_results_based_on_jurisdiction_location_and_state_filters() {

        int tasksToConfigure = 2;
        String[] ccdIds = new String[tasksToConfigure];

        String[] locationIds = {"17595", "17594"};

        for (int i = 0; i < tasksToConfigure; i++) {
            String ccdId = ccdIdGenerator.generate();
            ccdIds[i] = ccdId;

            CamundaProcessVariables processVariables = processVariables()
                .withProcessVariable("jurisdiction", "IA")
                .withProcessVariable("location", locationIds[i])
                .withProcessVariable("locationName", "A Hearing Centre")
                .withProcessVariable("taskState", "unassigned")
                .build();

            List<CamundaTask> tasks = given
                .iCreateATaskWithCcdId(ccdId)
                .and()
                .iRetrieveATaskWithProcessVariableFilter("ccdId", ccdId);

            String taskId = tasks.get(0).getId();

            given
                .iAddVariablesToTaskWithId(taskId, processVariables);

        }

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(LOCATION, SearchOperator.IN, asList("17595", "17594")),
            new SearchParameter(STATE, SearchOperator.IN, singletonList("unassigned"))
        ));

        Response result = restApiActions.post(
            "task",
            searchTaskRequest,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.state", everyItem(equalTo("unassigned")))
            .body("tasks.case_data.reference", hasItems(ccdIds[0], ccdIds[1]))
            .body("tasks.case_data.location.id", everyItem(either(is("17595")).or(is("17594"))));
    }

    private Map<String, String> setUpTaskWithCustomVariables(CamundaProcessVariables processVariables) {
        Map<String, String> task = common.setupTaskAndRetrieveIds();
        given.iAddVariablesToTaskWithId(task.get("taskId"), processVariables);
        return task;
    }

}

