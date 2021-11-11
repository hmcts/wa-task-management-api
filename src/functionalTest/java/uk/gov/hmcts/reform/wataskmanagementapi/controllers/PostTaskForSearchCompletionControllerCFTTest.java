package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;

@Slf4j
public class PostTaskForSearchCompletionControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";
    private static final String TASK_INITIATION_END_POINT = "task/{task-id}";
    private static final String CAMUNDA_SEARCH_HISTORY_ENDPOINT = "/history/variable-instance";

    private Headers authenticationHeaders;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2");
    }

    @Test
    public void given_processApplication_task_when_decideAnApplication_event_then_return_processApplication_tasks() {
        TestVariables processApplicationTaskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
            Map.of(
                CamundaVariableDefinition.TASK_TYPE, "processApplication",
                CamundaVariableDefinition.TASK_ID, "processApplication"
            ));

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            processApplicationTaskVariables.getCaseId(),
            "decideAnApplication",
            "IA",
            "Asylum"
        );

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(processApplicationTaskVariables.getCaseId(),
            processApplicationTaskVariables.getTaskId(), null, "processApplication");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(1))
            .body("tasks[0].type", equalTo("processApplication"));

        common.cleanUpTask(processApplicationTaskVariables.getTaskId());
    }

    @Test
    public void should_return_a_200_empty_list_when_the_user_is_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "processApplication");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Ignore
    @Test
    public void should_return_a_200_and_empty_list_when_task_does_not_have_required_permissions() {

        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        common.overrideTaskPermissions(taskId, "Manage");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event ", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Ignore
    @Test
    public void should_return_a_200_and_retrieve_single_task_when_one_of_the_task_does_not_have_required_permissions() {
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();
        common.overrideTaskPermissions(taskId1, "Manage");
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        final String taskId2 = tasksList.get(1).getId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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
            CamundaVariableDefinition.TASK_TYPE, "reviewTheAppeal",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewTheAppeal");

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

    @Ignore
    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee() {
        final String assigneeId = getAssigneeId(authenticationHeaders);

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

    @Ignore
    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee_with_warnings() {
        final String assigneeId = getAssigneeId(authenticationHeaders);

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessageWithWarnings(caseId);
        sendMessageWithWarnings(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        common.setupOrganisationalRoleAssignment(authenticationHeaders);
        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            authenticationHeaders
        );

        // search for completable
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            caseId, "requestRespondentEvidence", "IA", "Asylum");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewTheAppeal");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event", is(false))
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_event_id_does_not_match_not_ia() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "PROBATE", "GrantOfRepresentation");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewTheAppeal");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "createCaseSummary");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewRespondentEvidence");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride);
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "invalidCaseId", "requestCmaRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewRespondentEvidence");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewTheAppeal");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
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

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), authenticationHeaders);

        insertTaskInCftTaskDb(taskVariables.getCaseId(),
            taskId, null, "reviewTheAppeal");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0));

        common.cleanUpTask(taskId);
    }

    private String getAssigneeId(Headers headers) {
        return authorizationHeadersProvider.getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
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

        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariables(caseId);

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

        Map<String, CamundaValue<?>> processVariables = given.createDefaultTaskVariablesWithWarnings(caseId);

        variablesOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesOverride.get(key), "String")));

        given.iCreateATaskWithCustomVariables(processVariables);
    }

    private void insertTaskInCftTaskDb(String caseId, String taskId, UserInfo userInfo, String taskType) {
        String warnings = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                          + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())),
            new TaskAttribute(TASK_DUE_DATE,  CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now().plusDays(10))),
            new TaskAttribute(TASK_CASE_CATEGORY, "Protection"),
            new TaskAttribute(TASK_ROLE_CATEGORY, "LEGAL_OPERATIONS"),
            new TaskAttribute(TASK_HAS_WARNINGS, true),
            new TaskAttribute(TASK_WARNINGS, warnings),
            new TaskAttribute(TASK_AUTO_ASSIGNED, false)
        ));

        Response result = restApiActions.post(
            TASK_INITIATION_END_POINT,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }
}

