package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class PostTaskForSearchCompletionControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
    }

    @After
    public void cleanUp() {
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void given_processApplication_task_when_decideAnApplication_event_then_return_processApplication_tasks() {
        TestVariables processApplicationTaskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            Map.of(
                CamundaVariableDefinition.TASK_TYPE, "processApplication",
                CamundaVariableDefinition.TASK_ID, "processApplication"
            ),
            "IA",
            "Asylum");

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            processApplicationTaskVariables.getCaseId(),
            "decideAnApplication",
            "IA",
            "Asylum"
        );

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].type", equalTo("processApplication"));

        common.cleanUpTask(processApplicationTaskVariables.getTaskId());
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_empty_list_when_task_does_not_have_required_permissions() {

        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride,
                                                                                                "IA",
                                                                                                "Asylum");
        String taskId = taskVariables.getTaskId();

        String executePermission = "Manage";
        common.overrideTaskPermissions(taskId, executePermission);

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_retrieve_single_task_when_one_of_the_task_does_not_have_required_permissions() {
        final String caseId = given.iCreateACcdCase();

        // create 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);
        if (tasksList.size() != 2) {
            fail("2 tasks should be created for case id: " + caseId);
        }

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();
        String executePermission = "Manage";
        common.overrideTaskPermissions(taskId1, executePermission);
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        final String taskId2 = tasksList.get(1).getId();
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].id", equalTo(taskId2));

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_event_and_case_match() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride,
                                                                                                "IA",
                                                                                                "Asylum");
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].task_state", equalTo("unassigned"))
            .body("tasks[0].case_id", equalTo(taskVariables.getCaseId()))
            .body("tasks[0].id", equalTo(taskId))
            .body("tasks[0].type", equalTo("reviewTheAppeal"))
            .body("tasks[0].jurisdiction", equalTo("IA"))
            .body("tasks[0].case_type_id", equalTo("Asylum"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee() {
        final String assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].task_state", equalTo("assigned"))
            .body("tasks[0].case_id", equalTo(caseId))
            .body("tasks[0].id", equalTo(taskId2))
            .body("tasks[0].type", equalTo("reviewTheAppeal"))
            .body("tasks[0].jurisdiction", equalTo("IA"))
            .body("tasks[0].case_type_id", equalTo("Asylum"))
            .body("tasks[0].warnings", is(false));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "tasks[0].warning_list.values");

        assertTrue(actualWarnings.isEmpty());

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
    }

    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee_with_warnings() {
        final String assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessageWithWarnings(caseId);
        sendMessageWithWarnings(caseId);

        final List<CamundaTask> tasksList = iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].task_state", equalTo("assigned"))
            .body("tasks[0].case_id", equalTo(caseId))
            .body("tasks[0].id", equalTo(taskId2))
            .body("tasks[0].type", equalTo("reviewTheAppeal"))
            .body("tasks[0].jurisdiction", equalTo("IA"))
            .body("tasks[0].case_type_id", equalTo("Asylum"))
            .body("tasks[0].warnings", is(true));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "tasks[0].warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "Code1", "warningText", "Text1"),
            Map.of("warningCode", "Code2", "warningText", "Text2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
    }

    @Test
    public void should_return_a_200_and_retrieve_multiple_tasks_by_event_and_case_match_and_assignee() {
        final String assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 3 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 3);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        // assign user to taskId3
        final String taskId3 = tasksList.get(2).getId();
        restApiActions.post(
            "task/{task-id}/assign",
            taskId3,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(2))
            .body("tasks.id", hasItems(taskId2, taskId3))
            .body("tasks.case_id", everyItem(is(caseId)))
            .body("tasks.jurisdiction", everyItem(is("IA")))
            .body("tasks.case_type_id", everyItem(is("Asylum")))
            .body("tasks.task_state", everyItem(is("assigned")));

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
        common.cleanUpTask(taskId3);
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_event_id_does_not_match_not_ia() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "PROBATE", "GrantOfRepresentation");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));
        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_match_but_not_found() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "reviewHearingRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_caseId_correct_eventId_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "someEventId", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_empty_list_when_caseId_match_not_found() {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewReasonsForAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride,
                                                                                                "IA",
                                                                                                "Asylum");
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "invalidCaseId", "requestCmaRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_jurisdiction_is_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariable(JURISDICTION, "SSCS");
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "jurisdiction", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));


        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_caseType_is_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "caseType");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    private String getAssigneeId(Headers headers) {
        return authorizationProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
    }

    private void sendMessage(String caseId) {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_TYPE, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );

        Map<String, CamundaValue<?>> processVariables
            = given.createDefaultTaskVariables(caseId, "IA", "Asylum");

        variablesOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesOverride.get(key), "String")));

        given.iCreateATaskWithCustomVariables(processVariables);
    }

    private void sendMessageWithWarnings(String caseId) {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_TYPE, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );

        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariablesWithWarnings(caseId,
                                                                                                     "IA",
                                                                                                     "Asylum");

        variablesOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesOverride.get(key), "String")));

        given.iCreateATaskWithCustomVariables(processVariables);
    }

    private List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value, int taskCount) {
        String filter = "?processVariables=" + key + "_eq_" + value;

        AtomicReference<List<CamundaTask>> response = new AtomicReference<>();
        await().ignoreException(AssertionError.class)
            .pollInterval(1, SECONDS)
            .atMost(60, SECONDS)
            .until(
                () -> {
                    Response result = camundaApiActions.get(
                        "/task" + filter,
                        authorizationProvider.getServiceAuthorizationHeader()
                    );

                    result.then().assertThat()
                        .statusCode(HttpStatus.OK.value())
                        .contentType(APPLICATION_JSON_VALUE)
                        .body("size()", is(taskCount));

                    response.set(
                        result.then()
                            .extract()
                            .jsonPath().getList("", CamundaTask.class)
                    );

                    return true;
                });

        return response.get();
    }
}

