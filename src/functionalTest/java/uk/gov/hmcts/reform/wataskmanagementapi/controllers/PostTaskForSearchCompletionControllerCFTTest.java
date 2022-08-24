package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.JURISDICTION;

@Slf4j
public class PostTaskForSearchCompletionControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";
    private static final String TASK_INITIATION_END_POINT = "task/{task-id}";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_200_with_appropriate_task_to_complete() {
        Stream<CompletableTaskScenario> scenarios = tasksToCompleteScenarios();
        scenarios.forEach(scenario -> {

            TestVariables testVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(
                Map.of(
                    CamundaVariableDefinition.TASK_TYPE, scenario.taskId,
                    CamundaVariableDefinition.TASK_ID, scenario.taskId
                ),
                "IA",
                "Asylum");

            SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
                testVariables.getCaseId(),
                scenario.eventId,
                "IA",
                "Asylum"
            );

            common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

            common.insertTaskInCftTaskDb(testVariables, scenario.taskId, caseworkerCredentials.getHeaders());

            Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                decideAnApplicationSearchRequest,
                caseworkerCredentials.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(APPLICATION_JSON_VALUE)
                .body("task_required_for_event", is(false))
                .body("tasks.size()", equalTo(1))
                .body("tasks[0].permissions.values.size()", equalTo(5))
                .body("tasks[0].permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
                .body("tasks[0].type", equalTo(scenario.taskId))
                .body("tasks[0].work_type_id", equalTo(scenario.workTypeId))
                .body("tasks[0].work_type_label", equalTo(scenario.workTypeLabel))
                .body("tasks[0].role_category", equalTo(scenario.roleCategory));

            common.cleanUpTask(testVariables.getTaskId());
        });
    }

    @Test
    public void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        common.insertTaskInCftTaskDb(taskVariables, "processApplication", caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_required_for_event", is(false))
            .body("tasks.size()", equalTo(0));

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
        final String taskId = taskVariables.getTaskId();


        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        insertTaskInCftTaskDb(taskVariables.getCaseId(), taskVariables.getCaseId(), "UnknownEvent");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(),
            "requestRespondentEvidence",
            "IA",
            "Asylum");

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

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        final String taskId1 = tasksList.get(0).getId();

        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        insertTaskInCftTaskDb(caseId, taskId1, "UnknownEvent");

        // role created without completion permission
        final String taskId2 = tasksList.get(1).getId();
        insertTaskInCftTaskDb(caseId, taskId2, "reviewTheAppeal");
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
            .body("tasks[0].permissions.values.size()", equalTo(5))
            .body("tasks[0].permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
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
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum",
            CamundaVariableDefinition.DESCRIPTION, "aDescription"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride,
            "IA",
            "Asylum");
        String taskId = taskVariables.getTaskId();

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "reviewTheAppeal", caseworkerCredentials.getHeaders());

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
            .body("tasks[0].case_type_id", equalTo("Asylum"))
            .body("tasks[0].permissions.values.size()", equalTo(5))
            .body("tasks[0].permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"))
            .body("tasks[0].description", equalTo(
                "[Request respondent evidence](/case/IA/Asylum/${[CASE_REFERENCE]}/trigger/requestRespondentEvidence)"
            ))
            .body("tasks[0].role_category", equalTo("LEGAL_OPERATIONS"));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee() {
        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        final String assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());
        TestAuthenticationCredentials assignerHeaders =
            authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");

        common.setupOrganisationalRoleAssignment(assignerHeaders.getHeaders(), "task-supervisor");

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        insertTaskInCftTaskDb(caseId, taskId1, "UnknownEvent");

        final String taskId2 = tasksList.get(1).getId();
        insertTaskInCftTaskDb(caseId, taskId2, "reviewTheAppeal");

        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            assignerHeaders.getHeaders()
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
            .body("tasks[0].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("tasks[0].case_type_id", equalTo("Asylum"))
            .body("tasks[0].warnings", is(false))
            .body("tasks[0].permissions.values.size()", equalTo(5))
            .body("tasks[0].permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "tasks[0].warning_list.values");

        assertTrue(actualWarnings.isEmpty());

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
        common.clearAllRoleAssignments(assignerHeaders.getHeaders());
    }

    @Test
    public void should_return_a_200_and_retrieve_single_task_by_event_and_case_match_and_assignee_with_warnings() {

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());
        final String assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());
        TestAuthenticationCredentials assignerHeaders = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2");

        common.setupOrganisationalRoleAssignment(assignerHeaders.getHeaders(), "task-supervisor");

        // create a caseId
        final String caseId = given.iCreateACcdCase();

        // create a 2 tasks for caseId
        sendMessage(caseId);
        sendMessage(caseId);

        final List<CamundaTask> tasksList = given.iRetrieveATaskWithProcessVariableFilter("caseId", caseId, 2);

        // No user assigned to this task
        final String taskId1 = tasksList.get(0).getId();

        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        insertTaskInCftTaskDbWithWarnings(caseId, taskId1, "UnknownEvent");

        // assign user to taskId2
        final String taskId2 = tasksList.get(1).getId();
        insertTaskInCftTaskDbWithWarnings(caseId, taskId2, "reviewTheAppeal");

        // assign user to taskId2
        restApiActions.post(
            "task/{task-id}/assign",
            taskId2,
            new AssignTaskRequest(assigneeId),
            assignerHeaders.getHeaders()
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
            .body("tasks[0].warnings", is(true))
            .body("tasks[0].permissions.values.size()", equalTo(5))
            .body("tasks[0].permissions.values", hasItems("Read", "Refer", "Own", "Manage", "Cancel"));


        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "tasks[0].warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "Code1", "warningText", "Text1"),
            Map.of("warningCode", "Code2", "warningText", "Text2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);

        common.cleanUpTask(taskId1);
        common.cleanUpTask(taskId2);
        common.clearAllRoleAssignments(assignerHeaders.getHeaders());
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "reviewTheAppeal", caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
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
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "PROBATE", "GrantOfRepresentation");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "reviewTheAppeal", caseworkerCredentials.getHeaders());

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
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "reviewHearingRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "createCaseSummary", caseworkerCredentials.getHeaders());

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
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "someEventId", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(
            taskVariables,
            "reviewRespondentEvidence",
            caseworkerCredentials.getHeaders()
        );

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
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "invalidCaseId", "requestCmaRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(
            taskVariables,
            "reviewRespondentEvidence",
            caseworkerCredentials.getHeaders()
        );

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
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "jurisdiction",
            "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "reviewTheAppeal", caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0))
            .body("task_required_for_event", is(false));
        ;


        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_when_performing_search_when_caseType_is_incorrect() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "caseType");

        common.setupRestrictedRoleAssignment(taskVariables.getCaseId(), caseworkerCredentials.getHeaders());

        common.insertTaskInCftTaskDb(taskVariables, "reviewTheAppeal", caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            searchEventAndCase,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("tasks.size()", equalTo(0))
            .body("task_required_for_event", is(false));

        common.cleanUpTask(taskId);
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

        Map<String, CamundaValue<?>> processVariables
            = given.createDefaultTaskVariablesWithWarnings(caseId, "IA", "Asylum");

        variablesOverride.keySet()
            .forEach(key -> processVariables
                .put(key.value(), new CamundaValue<>(variablesOverride.get(key), "String")));

        given.iCreateATaskWithCustomVariables(processVariables);
    }

    private void insertTaskInCftTaskDbWithWarnings(String caseId, String taskId, String taskType) {
        String warnings = "[{\"warningCode\":\"Code1\", \"warningText\":\"Text1\"}, "
                          + "{\"warningCode\":\"Code2\", \"warningText\":\"Text2\"}]";

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())),
            new TaskAttribute(TASK_DUE_DATE, CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now().plusDays(10))),
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
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());
    }

    private void insertTaskInCftTaskDb(String caseId, String taskId, String taskType) {
        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, taskType),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, caseId),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now())),
            new TaskAttribute(TASK_DUE_DATE, CAMUNDA_DATA_TIME_FORMATTER.format(ZonedDateTime.now().plusDays(10))),
            new TaskAttribute(TASK_CASE_CATEGORY, "Protection"),
            new TaskAttribute(TASK_ROLE_CATEGORY, "LEGAL_OPERATIONS"),
            new TaskAttribute(TASK_AUTO_ASSIGNED, false)
        ));

        restApiActions.post(
            TASK_INITIATION_END_POINT,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );
    }

    private static Stream<CompletableTaskScenario> tasksToCompleteScenarios() {
        return Stream.of(
            new CompletableTaskScenario(
                "processApplication",
                "decideAnApplication",
                "applications",
                "Applications",
                "LEGAL_OPERATIONS",
                false
            ),
            new CompletableTaskScenario(
                "reviewAdditionalEvidence",
                "markEvidenceAsReviewed",
                "decision_making_work",
                "Decision-making work",
                "LEGAL_OPERATIONS",
                true
            ),
            new CompletableTaskScenario(
                "reviewAdditionalHomeOfficeEvidence",
                "markEvidenceAsReviewed",
                "decision_making_work",
                "Decision-making work",
                "LEGAL_OPERATIONS",
                true
            )
        );
    }

    @Getter
    @AllArgsConstructor
    private static class CompletableTaskScenario {
        private String taskId;
        private String eventId;
        private String workTypeId;
        private String workTypeLabel;
        private String roleCategory;
        private boolean taskRequiredForEvent;
    }
}

