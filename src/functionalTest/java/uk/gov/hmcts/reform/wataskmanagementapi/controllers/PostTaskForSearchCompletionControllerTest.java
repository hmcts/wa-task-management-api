package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.idam.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostTaskForSearchCompletionControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/completableByCaseEvent";
    private static final String ENDPOINT_COMPLETE_TASK = "task/{task-id}/complete";
    private String taskId;

    @Test
    public void should_return_a_401_when_the_user_is_unauthorised() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        taskId = task.get("taskId");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(task.get("caseId"), "requestRespondentEvidence");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authorizationHeadersProvider.getLawFirmBAuthorization()
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
    public void should_return_a_200_and_retrieve_a_task_by_event_and_case_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TYPE, "ReviewTheAppeal",
            CamundaVariableDefinition.TASK_ID, "ReviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned"
        );

        Map<String, String> task = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        taskId = task.get("taskId");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(task.get("caseId"), "requestRespondentEvidence");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks[0].task_state", equalTo("unassigned"))
            .body("tasks[0].case_id", equalTo(task.get("caseId")))
            .body("tasks[0].id", equalTo(task.get("taskId")))
            .body("tasks[0].type", equalTo("ReviewTheAppeal"));
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        taskId = task.get("taskId");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(task.get("caseId"), "no_event_id");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_match_but_not_found() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        taskId = task.get("taskId");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(task.get("caseId"), "reviewHearingRequirements");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authorizationHeadersProvider.getTribunalCaseworkerBAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_caseId_correct_eventId_incorrect() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);
        taskId = task.get("taskId");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(task.get("caseId"), null);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
    }

    @After
    public void cleanUp() {
        camundaApiActions.post(ENDPOINT_COMPLETE_TASK, taskId,
                               new Headers(authorizationHeadersProvider.getServiceAuthorizationHeader()));
    }
}

