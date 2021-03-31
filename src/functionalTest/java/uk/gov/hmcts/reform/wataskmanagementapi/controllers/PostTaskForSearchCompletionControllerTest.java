package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.Common.REASON_COMPLETED;

public class PostTaskForSearchCompletionControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization();
        common.clearAllRoleAssignments(authenticationHeaders);
    }

    @Test
    public void should_return_a_401_when_the_user_is_unauthorised() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "ia", "asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
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

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_event_and_case_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            //The task-configuration-api set this var to this location automatically
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TYPE, "ReviewTheAppeal",
            CamundaVariableDefinition.TASK_ID, "ReviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );

        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "ia", "asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        await()
            .ignoreException(AssertionError.class)
            .atMost(6, TimeUnit.SECONDS) // retry three times
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> {

                Response result = restApiActions.post(
                    ENDPOINT_BEING_TESTED,
                    searchEventAndCase,
                    authenticationHeaders
                );

                result.then().assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .contentType(APPLICATION_JSON_VALUE)
                    .body("tasks[0].task_state", equalTo("unassigned"))
                    .body("tasks[0].case_id", equalTo(taskVariables.getCaseId()))
                    .body("tasks[0].id", equalTo(taskId))
                    .body("tasks[0].type", equalTo("ReviewTheAppeal"))
                    .body("tasks[0].jurisdiction", equalTo("IA"))
                    .body("tasks[0].case_type_id", equalTo("Asylum"));

                return true;

            });

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "ia", "asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_400_and_when_event_id_does_not_match_not_ia() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "PROBATE", "GrantOfRepresentation");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("message", equalTo("Please check your request. "
                                         + "This endpoint currently only supports "
                                         + "the Immigration & Asylum service"));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_match_but_not_found() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "reviewHearingRequirements", "ia", "asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_caseId_correct_eventId_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "someEventId", "ia", "asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_400_and_when_performing_search_when_jurisdiction_is_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(JURISDICTION, "SSCS");
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "jurisdiction", "asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

    @Test
    public void should_return_a_400_and_when_performing_search_when_caseType_is_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "caseType");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        common.cleanUpTask(taskId, REASON_COMPLETED);
    }

}

