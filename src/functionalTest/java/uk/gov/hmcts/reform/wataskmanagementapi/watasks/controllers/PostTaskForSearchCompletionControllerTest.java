package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@Slf4j
public class PostTaskForSearchCompletionControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";

    @Before
    public void setUp() {
        waCaseworkerCredentials = authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(waCaseworkerCredentials.getAccount().getUsername());

        common.clearAllRoleAssignments(baseCaseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(baseCaseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_200_with_appropriate_task_to_complete() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());

        Stream<CompletableTaskScenario> scenarios = tasksToCompleteScenarios();
        scenarios.forEach(scenario -> {

            TestVariables testVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                           "processApplication",
                                                                           "process application");
            initiateTask(testVariables);

            SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
                testVariables.getCaseId(),
                scenario.eventId,
                WA_JURISDICTION,
                WA_CASE_TYPE
            );

            Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                decideAnApplicationSearchRequest,
                waCaseworkerCredentials.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("task_required_for_event", is(scenario.taskRequiredForEvent))
                .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
                .body("tasks.id", everyItem(is(equalTo(testVariables.getTaskId()))))
                .body("tasks.name", everyItem(equalTo("process application")))
                .body("tasks.type", everyItem(equalTo("processApplication")))
                .body("tasks.task_state", everyItem(equalTo("unassigned")))
                .body("tasks.task_system", everyItem(equalTo("SELF")))
                .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
                .body("tasks.task_title", everyItem(equalTo("process application")))
                .body("tasks.created_date", everyItem(notNullValue()))
                .body("tasks.due_date", everyItem(notNullValue()))
                .body("tasks.location_name", everyItem(equalTo("Taylor House")))
                .body("tasks.location", everyItem(equalTo("765324")))
                .body("tasks.execution_type", everyItem(equalTo("Case Management Task")))
                .body("tasks.jurisdiction", everyItem(equalTo("WA")))
                .body("tasks.region", everyItem(equalTo("1")))
                .body("tasks.case_type_id", everyItem(equalTo("WaCaseType")))
                .body("tasks.case_id", everyItem(is(equalTo(testVariables.getCaseId()))))
                .body("tasks.case_category", everyItem(equalTo("Protection")))
                .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
                .body("tasks.auto_assigned", everyItem(equalTo(false)))
                .body("tasks.warnings", everyItem(equalTo(false)))
                .body("tasks.case_management_category", everyItem(equalTo("Protection")))
                .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
                .body("tasks.permissions.values",
                    everyItem(equalToObject(List.of("Read", "Own", "Manage", "CompleteOwn", "CancelOwn", "Claim"))))
                .body(
                    "tasks.description",
                    everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                          + "trigger/decideAnApplication)"))
                )
                .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
                .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))))
                .body("tasks.next_hearing_id", everyItem(equalTo("next-hearing-id")))
                .body("tasks.next_hearing_date", everyItem(notNullValue()))
                .body("tasks.priority_date", everyItem(notNullValue()))
                .body("tasks.minor_priority", everyItem(equalTo(500)))
                .body("tasks.major_priority", everyItem(equalTo(1000)));

            common.cleanUpTask(testVariables.getTaskId());
        });
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_event_id_does_not_match() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        TestVariables testVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication",
                                                                       "process application");
        initiateTask(testVariables);

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            testVariables.getCaseId(),
            "UnknownEvent",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(0));

        common.cleanUpTask(testVariables.getTaskId());
    }

    @Test
    public void should_return_a_200_and_empty_list_when_caseId_match_not_found() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        TestVariables testVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication",
                                                                       "process application");
        initiateTask(testVariables);

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            "invalidCaseId",
            "decideAnApplication",
            WA_JURISDICTION,
            WA_CASE_TYPE
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(0));

        common.cleanUpTask(testVariables.getTaskId());
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_dmn_jurisdiction_not_match() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        TestVariables testVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication",
                                                                       "process application");
        initiateTask(testVariables);

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            testVariables.getCaseId(),
            "decideAnApplication",
            "PROBATE",
            WA_CASE_TYPE
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(0));

        common.cleanUpTask(testVariables.getTaskId());
    }

    @Test
    public void should_return_a_200_and_return_and_empty_list_when_dmn_case_type_not_match() {
        common.setupWAOrganisationalRoleAssignment(waCaseworkerCredentials.getHeaders());
        TestVariables testVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json",
                                                                       "processApplication",
                                                                       "process application");
        initiateTask(testVariables);

        SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
            testVariables.getCaseId(),
            "decideAnApplication",
            WA_JURISDICTION,
            "GrantOfRepresentation"
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            decideAnApplicationSearchRequest,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(0));

        common.cleanUpTask(testVariables.getTaskId());
    }


    private static Stream<CompletableTaskScenario> tasksToCompleteScenarios() {
        return Stream.of(
            new CompletableTaskScenario(
                "processApplication",
                "decideAnApplication",
                "applications",
                "LEGAL_OPERATIONS",
                false
            )
        );
    }

    @Getter
    @AllArgsConstructor
    private static class CompletableTaskScenario {
        private String taskId;
        private String eventId;
        private String workTypeId;
        private String roleCategory;
        private boolean taskRequiredForEvent;
    }
}

