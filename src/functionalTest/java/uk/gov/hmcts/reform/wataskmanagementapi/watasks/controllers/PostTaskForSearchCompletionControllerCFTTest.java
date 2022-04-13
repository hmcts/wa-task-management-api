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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

@Slf4j
public class PostTaskForSearchCompletionControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/search-for-completable";
    private static final String TASK_INITIATION_ENDPOINT = "task/{task-id}";

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_200_with_appropriate_task_to_complete() {
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Stream<CompletableTaskScenario> scenarios = tasksToCompleteScenarios();
        scenarios.forEach(scenario -> {

            TestVariables testVariables = createWaTask();

            SearchEventAndCase decideAnApplicationSearchRequest = new SearchEventAndCase(
                testVariables.getCaseId(),
                scenario.eventId,
                "WA",
                "WaCaseType"
            );

            Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                decideAnApplicationSearchRequest,
                caseworkerCredentials.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("task_required_for_event", is(scenario.taskRequiredForEvent))
                .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
                .body("tasks.id", everyItem(is(equalTo(testVariables.getTaskId()))))
                .body("tasks.name", everyItem(equalTo("process Application")))
                .body("tasks.type", everyItem(equalTo("processApplication")))
                .body("tasks.task_state", everyItem(equalTo("unassigned")))
                .body("tasks.task_system", everyItem(equalTo("SELF")))
                .body("tasks.security_classification", everyItem(equalTo("PUBLIC")))
                .body("tasks.task_title", everyItem(equalTo("process Application")))
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
                .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Refer", "Execute"))))
                .body("tasks.description",
                    everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                      + "trigger/decideAnApplication)")))
                .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
                .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))));

            common.cleanUpTask(testVariables.getTaskId());
        });
    }

    private TestVariables createWaTask() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "processApplication"),
            new TaskAttribute(TASK_NAME, "process Application"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "process Application"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response initiationResponse = restApiActions.post(
            TASK_INITIATION_ENDPOINT,
            taskId,
            req,
            caseworkerCredentials.getHeaders()
        );

        initiationResponse.prettyPrint();
        return taskVariables;
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

