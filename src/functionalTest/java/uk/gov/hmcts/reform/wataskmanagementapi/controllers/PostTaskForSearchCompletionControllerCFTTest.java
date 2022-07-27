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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
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

            initiateTask(testVariables, Jurisdiction.IA);

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
                .body("tasks[0].permissions.values", hasItems("Read", "Own", "Manage", "Cancel"))
                .body("tasks[0].type", equalTo(scenario.taskId))
                .body("tasks[0].work_type_id", equalTo(scenario.workTypeId))
                .body("tasks[0].work_type_label", equalTo(scenario.workTypeLabel))
                .body("tasks[0].role_category", equalTo(scenario.roleCategory));

            common.cleanUpTask(testVariables.getTaskId());
        });
    }

    @Test
    public void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("processApplication");
        String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "requestRespondentEvidence", "IA", "Asylum");

        initiateTask(taskVariables, Jurisdiction.IA);

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

        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, "UnknownEvent",
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum",
            CamundaVariableDefinition.TASK_TYPE, "UnknownEvent"
        );
        TestVariables taskVariables = common.setupTaskAndRetrieveIdsWithCustomVariablesOverride(variablesOverride,
            "IA",
            "Asylum");
        final String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables, Jurisdiction.IA);

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

        // create 2 tasks for caseId
        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        TestVariables taskVariables1 = sendMessage(caseId, "UnknownEvent", 1);
        TestVariables taskVariables2 = sendMessage(caseId, "reviewTheAppeal", 2);

        final String taskId1 = taskVariables1.getTaskId();
        initiateTask(taskVariables1, Jurisdiction.IA);

        // role created without completion permission
        final String taskId2 = taskVariables2.getTaskId();
        initiateTask(taskVariables2, Jurisdiction.IA);

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
            .body("tasks[0].permissions.values", hasItems("Read", "Own", "Manage", "Cancel"))
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

        initiateTask(taskVariables, Jurisdiction.IA);

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
            .body("tasks[0].permissions.values", hasItems("Read", "Own", "Manage", "Cancel"))
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
        TestVariables taskVariables1 = sendMessage(caseId, "UnknownEvent", 1);
        TestVariables taskVariables2 = sendMessage(caseId, "reviewTheAppeal",2);

        // No user assigned to this task
        final String taskId1 = taskVariables1.getTaskId();

        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        initiateTask(taskVariables1, Jurisdiction.IA);

        final String taskId2 = taskVariables2.getTaskId();
        initiateTask(taskVariables2, Jurisdiction.IA);

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
            .body("tasks[0].permissions.values", hasItems("Read", "Own", "Manage", "Cancel"));

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

        // create 2 tasks for caseId
        // The UnknownEvent event is used to test so that permissions table is matched only to
        // "Read,Refer,Manage,Cancel" Rest of the events has either Own or Execute
        TestVariables taskVariables1 = sendMessageWithWarnings(caseId, "UnknownEvent", 1);
        TestVariables taskVariables2 = sendMessageWithWarnings(caseId, "reviewTheAppeal", 2);

        // No user assigned to this task
        final String taskId1 = taskVariables1.getTaskId();
        initiateTask(taskVariables1, Jurisdiction.IA);

        // assign user to taskId2
        final String taskId2 = taskVariables2.getTaskId();
        initiateTask(taskVariables2, Jurisdiction.IA);

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
            .body("tasks[0].permissions.values", hasItems("Read", "Own", "Manage", "Cancel"));


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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("reviewTheAppeal");
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.IA);

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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("reviewTheAppeal");
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "solicitorCreateApplication", "PROBATE", "GrantOfRepresentation");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.IA);

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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("createCaseSummary");
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "reviewHearingRequirements", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.IA);

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
        TestVariables taskVariables = common.setupTaskAndRetrieveIds("reviewRespondentEvidence");
        final String taskId = taskVariables.getTaskId();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            taskVariables.getCaseId(), "someEventId", "IA", "Asylum");

        common.setupOrganisationalRoleAssignment(caseworkerCredentials.getHeaders());

        initiateTask(taskVariables, Jurisdiction.IA);

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
            CamundaVariableDefinition.TASK_TYPE, "reviewRespondentEvidence",
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

        initiateTask(taskVariables, Jurisdiction.IA);

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

        initiateTask(taskVariables, Jurisdiction.IA);

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

        initiateTask(taskVariables, Jurisdiction.IA);

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

    private TestVariables sendMessage(String caseId, String taskType, int taskIndex) {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, taskType,
            CamundaVariableDefinition.TASK_TYPE, taskType,
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );

        return common.setupTaskWithCaseIdAndRetrieveIdsWithCustomVariablesOverride(variablesOverride, caseId,
                                                                                   "IA", "Asylum",
                                                                                   taskIndex);
    }

    private TestVariables sendMessageWithWarnings(String caseId, String taskType, int taskIndex) {
        Map<CamundaVariableDefinition, String> variablesOverride = Map.of(
            CamundaVariableDefinition.JURISDICTION, "IA",
            CamundaVariableDefinition.LOCATION, "765324",
            CamundaVariableDefinition.TASK_ID, taskType,
            CamundaVariableDefinition.TASK_TYPE, taskType,
            CamundaVariableDefinition.TASK_STATE, "unassigned",
            CamundaVariableDefinition.CASE_TYPE_ID, "Asylum"
        );

        WarningValues warnings = new WarningValues(
            asList(
                new Warning("Code1", "Text1"),
                new Warning("Code2", "Text2")
            ));

        return common.setupTaskWithCaseIdAndRetrieveIdsWithCustomVariablesOverride(variablesOverride, caseId,
                                                                                   "IA", "Asylum",
                                                                                   warnings, taskIndex);
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

