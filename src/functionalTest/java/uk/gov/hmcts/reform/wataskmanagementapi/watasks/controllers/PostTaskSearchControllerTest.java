package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;

@SuppressWarnings("checkstyle:LineLength")
public class PostTaskSearchControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task";
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
    public void should_return_a_200_with_search_results_and_correct_properties() {
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        List<TestVariables> tasksCreated = new ArrayList<>();

        TestVariables testVariables = createWaTask();
        tasksCreated.add(testVariables);

        testVariables = createWaTask();
        tasksCreated.add(testVariables);

        List<String> taskIds = tasksCreated.stream().map(TestVariables::getTaskId).collect(Collectors.toList());
        List<String> caseIds = tasksCreated.stream().map(TestVariables::getCaseId).collect(Collectors.toList());

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("WA")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("765324")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, false),
            new SearchParameterList(CASE_ID, SearchOperator.IN, caseIds)
        ));

        tasksCreated.forEach(testVariable ->
            common.insertTaskInCftTaskDb(testVariable, "processApplication", caseworkerCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED + "?first_result=0&max_results=10",
            searchTaskRequest,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("tasks.size()", lessThanOrEqualTo(10)) //Default max results
            .body("tasks.id", everyItem(notNullValue()))
            .body("tasks.id", hasItem(is(in(taskIds))))
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
            .body("tasks.case_id", hasItem(is(in(caseIds))))
            .body("tasks.case_category", everyItem(equalTo("Protection")))
            .body("tasks.case_name", everyItem(equalTo("Bob Smith")))
            .body("tasks.auto_assigned", everyItem(equalTo(false)))
            .body("tasks.warnings", everyItem(equalTo(false)))
            .body("tasks.case_management_category", everyItem(equalTo("Protection")))
            .body("tasks.work_type_id", everyItem(equalTo("hearing_work")))
            .body("tasks.permissions.values", everyItem(equalToObject(List.of("Read", "Refer", "Execute"))))
            .body("tasks.description", everyItem(equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                         + "trigger/decideAnApplication)")))
            .body("tasks.role_category", everyItem(equalTo("LEGAL_OPERATIONS")))
            .body("tasks.additional_properties", everyItem(equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            ))))
            .body("total_records", equalTo(2));

        tasksCreated
            .forEach(task -> common.cleanUpTask(task.getTaskId()));
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

}
