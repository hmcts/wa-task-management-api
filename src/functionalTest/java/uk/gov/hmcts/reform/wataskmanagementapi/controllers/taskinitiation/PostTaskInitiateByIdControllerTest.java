package uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

public class PostTaskInitiateByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private Headers authenticationHeaders;

    private static final Stream<String> caseManagerAndTribunalCaseworkerTasks =
        Stream.of("reviewRespondentEvidence", "followUpOverdueRespondentEvidence",
                  "reviewAppealSkeletonArgument", "followUpOverdueCaseBuilding", "reviewReasonsForAppeal",
                  "followUpOverdueReasonsForAppeal", "reviewClarifyingQuestionsAnswers",
                  "followUpOverdueClarifyingAnswers", "reviewRespondentResponse",
                  "followUpOverdueRespondentReview", "reviewHearingRequirements",
                  "followUpOverdueHearingRequirements", "reviewCmaRequirements",
                  "reviewAdditionalHomeOfficeEvidence", "reviewAdditionalAppellantEvidence",
                  "reviewAdditionalHomeOfficeEvidence", "reviewAdditionalAppellantEvidence",
                  "createHearingBundle"
        );

    private static final Stream<String> nationalBusinessCentreTasks
        = Stream.of("arrangeOfflinePayment", "markCaseAsPaid", "addListingDate");

    private static final Stream<String> hearingCentreTasks = Stream.of("allocateHearingJudge", "uploadHearingRecording");

    private static final Stream<String> judgeTasks =
        Stream.of("reviewHearingBundle", "generateDraftDecisionAndReasons", "uploadDecision",
                  "reviewAddendumHomeOfficeEvidence", "reviewAddendumAppellantEvidence", "reviewAddendumEvidence"
        );

    @Before
    public void setUp() {
        //Reset role assignments
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-");
        common.clearAllRoleAssignments(authenticationHeaders);
    }

    @Test
    public void should_return_a_201_when_initiating_a_judge_task_by_id() {
        if (isR2FeatureEnabled()) {
            judgeTasks.forEach(taskTypeId -> {
                TestVariables taskVariables = common.setupTaskAndRetrieveIds();
                String taskId = taskVariables.getTaskId();
                common.setupOrganisationalRoleAssignment(authenticationHeaders);

                InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
                    new TaskAttribute(TASK_TYPE, taskTypeId),
                    new TaskAttribute(TASK_NAME, "aTaskName"),
                    new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
                    new TaskAttribute(TASK_TITLE, "A test task")
                ));

                Response result = restApiActions.post(
                    ENDPOINT_BEING_TESTED,
                    taskId,
                    req,
                    authenticationHeaders
                );

                result.prettyPrint();
                result.then().assertThat()
                    .statusCode(HttpStatus.CREATED.value())
                    .and()
                    .body("task_id", equalTo(taskId))
                    .body("task_name", equalTo("aTaskName"))
                    .body("task_type", equalTo(taskTypeId))
                    .body("state", equalTo("UNASSIGNED"))
                    .body("task_system", equalTo("SELF"))
                    .body("security_classification", equalTo("PUBLIC"))
                    .body("title", equalTo("aTaskName"))
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
                    .body("task_role_resources.size()", equalTo(3));

                assertPermissions(
                    getTaskResource(result, "hearing-judge"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "JUDICIAL",
                           "auto_assignable", true
                    )
                );
                assertPermissions(
                    getTaskResource(result, "task-supervisor"),
                    Map.of("read", true,
                           "refer", true,
                           "own", false,
                           "manage", true,
                           "execute", false,
                           "cancel", true,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "auto_assignable", false
                    )
                );
                assertPermissions(
                    getTaskResource(result, "judge"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "JUDICIAL",
                           "auto_assignable", false
                    )
                );

                assertions.taskVariableWasUpdated(
                    taskVariables.getProcessInstanceId(),
                    "cftTaskState",
                    "unassigned"
                );

                common.cleanUpTask(taskId);
            });
        }
    }

    @Test
    public void should_return_a_201_when_initiating_a_hearing_centre_admin_task_by_id() {
        if (isR2FeatureEnabled()) {
            hearingCentreTasks.forEach(taskTypeId -> {
                TestVariables taskVariables = common.setupTaskAndRetrieveIds();
                String taskId = taskVariables.getTaskId();
                common.setupOrganisationalRoleAssignment(authenticationHeaders);

                InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
                    new TaskAttribute(TASK_TYPE, taskTypeId),
                    new TaskAttribute(TASK_NAME, "aTaskName"),
                    new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
                    new TaskAttribute(TASK_TITLE, "A test task")
                ));

                Response result = restApiActions.post(
                    ENDPOINT_BEING_TESTED,
                    taskId,
                    req,
                    authenticationHeaders
                );

                result.prettyPrint();
                result.then().assertThat()
                    .statusCode(HttpStatus.CREATED.value())
                    .and()
                    .body("task_id", equalTo(taskId))
                    .body("task_name", equalTo("aTaskName"))
                    .body("task_type", equalTo(taskTypeId))
                    .body("state", equalTo("UNASSIGNED"))
                    .body("task_system", equalTo("SELF"))
                    .body("security_classification", equalTo("PUBLIC"))
                    .body("title", equalTo("aTaskName"))
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
                    .body("task_role_resources.size()", equalTo(2));

                assertPermissions(
                    getTaskResource(result, "hearing-centre-admin"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "ADMINISTRATOR",
                           "auto_assignable", false
                    )
                );

                assertPermissions(
                    getTaskResource(result, "task-supervisor"),
                    Map.of("read", true,
                           "refer", true,
                           "own", false,
                           "manage", true,
                           "execute", false,
                           "cancel", true,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "auto_assignable", false
                    )
                );

                assertions.taskVariableWasUpdated(
                    taskVariables.getProcessInstanceId(),
                    "cftTaskState",
                    "unassigned"
                );

                common.cleanUpTask(taskId);
            });
        }
    }

    @Test
    public void should_return_a_201_when_initiating_a_national_business_centre_task_by_id() {
        if (isR2FeatureEnabled()) {
            nationalBusinessCentreTasks.forEach(taskTypeId -> {
                TestVariables taskVariables = common.setupTaskAndRetrieveIds();
                String taskId = taskVariables.getTaskId();
                common.setupOrganisationalRoleAssignment(authenticationHeaders);

                InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
                    new TaskAttribute(TASK_TYPE, taskTypeId),
                    new TaskAttribute(TASK_NAME, "aTaskName"),
                    new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
                    new TaskAttribute(TASK_TITLE, "A test task")
                ));

                Response result = restApiActions.post(
                    ENDPOINT_BEING_TESTED,
                    taskId,
                    req,
                    authenticationHeaders
                );

                result.prettyPrint();
                result.then().assertThat()
                    .statusCode(HttpStatus.CREATED.value())
                    .and()
                    .body("task_id", equalTo(taskId))
                    .body("task_name", equalTo("aTaskName"))
                    .body("task_type", equalTo(taskTypeId))
                    .body("state", equalTo("UNASSIGNED"))
                    .body("task_system", equalTo("SELF"))
                    .body("security_classification", equalTo("PUBLIC"))
                    .body("title", equalTo("aTaskName"))
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
                    .body("task_role_resources.size()", equalTo(2));

                assertPermissions(
                    getTaskResource(result, "national-business-centre"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "ADMINISTRATOR",
                           "auto_assignable", false
                    )
                );
                assertPermissions(
                    getTaskResource(result, "task-supervisor"),
                    Map.of("read", true,
                           "refer", true,
                           "own", false,
                           "manage", true,
                           "execute", false,
                           "cancel", true,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "auto_assignable", false
                    )
                );

                assertions.taskVariableWasUpdated(
                    taskVariables.getProcessInstanceId(),
                    "cftTaskState",
                    "unassigned"
                );

                common.cleanUpTask(taskId);
            });
        }
    }

    @Test
    public void should_return_a_201_when_initiating_a_case_manager_and_tribunal_caseworker_task_by_id() {
        if (isR2FeatureEnabled()) {
            caseManagerAndTribunalCaseworkerTasks.forEach(taskTypeId -> {
                TestVariables taskVariables = common.setupTaskAndRetrieveIds();
                String taskId = taskVariables.getTaskId();
                common.setupOrganisationalRoleAssignment(authenticationHeaders);

                InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
                    new TaskAttribute(TASK_TYPE, taskTypeId),
                    new TaskAttribute(TASK_NAME, "aTaskName"),
                    new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
                    new TaskAttribute(TASK_TITLE, "A test task")
                ));

                Response result = restApiActions.post(
                    ENDPOINT_BEING_TESTED,
                    taskId,
                    req,
                    authenticationHeaders
                );

                result.prettyPrint();
                result.then().assertThat()
                    .statusCode(HttpStatus.CREATED.value())
                    .and()
                    .body("task_id", equalTo(taskId))
                    .body("task_name", equalTo("aTaskName"))
                    .body("task_type", equalTo(taskTypeId))
                    .body("state", equalTo("UNASSIGNED"))
                    .body("task_system", equalTo("SELF"))
                    .body("security_classification", equalTo("PUBLIC"))
                    .body("title", equalTo("aTaskName"))
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
                    .body("task_role_resources.size()", equalTo(3));

                assertPermissions(
                    getTaskResource(result, "tribunal-caseworker"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "LEGAL_OPERATIONS",
                           "auto_assignable", true
                    )
                );
                assertPermissions(
                    getTaskResource(result, "task-supervisor"),
                    Map.of("read", true,
                           "refer", true,
                           "own", false,
                           "manage", true,
                           "execute", false,
                           "cancel", true,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "auto_assignable", false
                    )
                );
                assertPermissions(
                    getTaskResource(result, "case-manager"),
                    Map.of("read", true,
                           "refer", true,
                           "own", true,
                           "manage", false,
                           "execute", false,
                           "cancel", false,
                           "task_id", taskId,
                           "authorizations", List.of("IA"),
                           "role_category", "LEGAL_OPERATIONS",
                           "auto_assignable", true
                    )
                );

                assertions.taskVariableWasUpdated(
                    taskVariables.getProcessInstanceId(),
                    "cftTaskState",
                    "unassigned"
                );

                common.cleanUpTask(taskId);
            });
        }
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
        return resources.get(0);
    }

    @Test
    public void should_return_a_201_when_initiating_a_default_task_by_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("aTaskName"))
            .body("task_type", equalTo("aTaskType"))
            .body("state", equalTo("UNASSIGNED"))
            .body("task_system", equalTo("SELF"))
            .body("security_classification", equalTo("PUBLIC"))
            .body("title", equalTo("aTaskName"))
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
            );

        if (isR2FeatureEnabled()) {

            assertPermissions(
                getTaskResource(result, "task-supervisor"),
                Map.of("read", true,
                       "refer", true,
                       "own", false,
                       "manage", true,
                       "execute", false,
                       "cancel", true,
                       "task_id", taskId,
                       "authorizations", List.of("IA"),
                       "auto_assignable", false
                )
            );
        } else {

            assertPermissions(
                getTaskResource(result, "tribunal-caseworker"),
                Map.of("read", true,
                       "refer", true,
                       "own", true,
                       "manage", true,
                       "execute", false,
                       "cancel", true,
                       "task_id", taskId,
                       "authorizations", List.of("IA"),
                       "role_category", "LEGAL_OPERATIONS",
                       "auto_assignable", false
                )
            );

            assertPermissions(
                getTaskResource(result, "senior-tribunal-caseworker"),
                Map.of("read", true,
                       "refer", true,
                       "own", true,
                       "manage", true,
                       "execute", false,
                       "cancel", true,
                       "task_id", taskId,
                       "authorizations", List.of("IA"),
                       "role_category", "LEGAL_OPERATIONS",
                       "auto_assignable", false
                )
            );
        }
        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_503_if_task_already_initiated() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));
        //First call
        Response resultFirstCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        resultFirstCall.then().assertThat()
            .statusCode(HttpStatus.CREATED.value());

        //Second call
        Response resultSecondCall = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        // If the first call succeeded the second call should throw a conflict
        // taskId unique constraint is violated
        resultSecondCall.then().assertThat()
            .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo(
                "https://github.com/hmcts/wa-task-management-api/problem/database-conflict"))
            .body("title", equalTo("Database Conflict Error"))
            .body("status", equalTo(503))
            .body("detail", equalTo(
                "Database Conflict Error: The action could not be completed because "
                    + "there was a conflict in the database."));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_500_if_no_case_id() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        String taskId = UUID.randomUUID().toString();

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));
        //First call
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_case_id_is_invalid() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, "someInvalidCaseID"),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }

    @Test
    public void should_return_a_500_if_task_id_does_not_exist() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        InitiateTaskRequest req = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task")
        ));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            UUID.randomUUID().toString(),
            req,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_PROBLEM_JSON_VALUE);
    }
}

