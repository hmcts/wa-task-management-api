package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers;

import io.restassured.http.Headers;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class PostTaskInitiateByIdForWAControllerTest extends SpringBootFunctionalBaseTest {
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2");
    }

    @Test
    public void should_return_a_201_when_initiating_a_judge_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(authenticationHeaders);

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "reviewHearingBundle"),
            new TaskAttribute(TASK_NAME, "review Hearing Bundle"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "review Hearing Bundle"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        //Note: this is the TaskResource.class
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("review Hearing Bundle"))
            .body("task_type", equalTo("reviewHearingBundle"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("review Hearing Bundle"))
            .body("created", notNullValue())
            .body("due_date_time", notNullValue())
            .body("auto_assigned", equalTo(false))
            .body("has_warnings", equalTo(false))
            .body("case_id", equalTo(taskVariables.getCaseId()))
            .body("case_type_id", equalTo("Asylum"))
            .body("case_name", equalTo("Bob Smith"))
            .body("case_category", equalTo("Protection"))
            .body("jurisdiction", equalTo("IA"))
            .body("region", equalTo("1"))
            .body("location", equalTo("765324"))
            .body("location_name", equalTo("Taylor House"))
            .body("execution_type_code.execution_code", equalTo("CASE_EVENT"))
            .body("execution_type_code.execution_name", equalTo("Case Management Task"))
            .body(
                "execution_type_code.description",
                equalTo("The task requires a case management event to be executed by the user. "
                            + "(Typically this will be in CCD.)")
            )
            .body("work_type_resource.id", equalTo("hearing_work"))
            .body("work_type_resource.label", equalTo("Hearing work"))
            .body("task_role_resources.size()", equalTo(5));

        assertPermissions(
            getTaskResource(result, "task-supervisor"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", true),
                entry("execute", false),
                entry("cancel", true),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("auto_assignable", false)
            )
        );
        assertPermissions(
            getTaskResource(result, "hearing-judge"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", false),
                entry("execute", false),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of("IA")),
                entry("role_category", "JUDICIAL"),
                entry("auto_assignable", true),
                entry("assignment_priority", 1)
            )
        );
        assertPermissions(
            getTaskResource(result, "judge"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", true),
                entry("manage", false),
                entry("execute", false),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of("IA")),
                entry("role_category", "JUDICIAL"),
                entry("auto_assignable", false),
                entry("assignment_priority", 1)
            )
        );

        assertPermissions(
            getTaskResource(result, "senior-tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );
        assertPermissions(
            getTaskResource(result, "tribunal-caseworker"),
            Map.ofEntries(
                entry("read", true),
                entry("refer", true),
                entry("own", false),
                entry("manage", false),
                entry("execute", true),
                entry("cancel", false),
                entry("task_id", taskId),
                entry("authorizations", List.of()),
                entry("role_category", "LEGAL_OPERATIONS"),
                entry("auto_assignable", false),
                entry("assignment_priority", 2)
            )
        );

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }


    private void assertPermissions(Map<String, Object> resource, Map<String, Object> expectedPermissions) {
        expectedPermissions.keySet().forEach(key ->
                                                 assertThat(resource).containsEntry(key, expectedPermissions.get(key)));

        assertThat(resource.get("task_role_id")).isNotNull();
    }

    private Map<String, Object> getTaskResource(Response result, String roleName) {
        final List<Map<String, Object>> resources = new JsonPath(result.getBody().asString())
            .param("roleName", roleName)
            .get("task_role_resources.findAll { resource -> resource.role_name == roleName }");
        return resources.size() > 0 ? resources.get(0) : null;
    }
}
