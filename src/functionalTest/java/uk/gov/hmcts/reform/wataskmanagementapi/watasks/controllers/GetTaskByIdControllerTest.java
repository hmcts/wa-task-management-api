package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;

public class GetTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";
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
    public void should_return_a_200_with_task_and_correct_properties() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.task_system", equalTo("SELF"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.task_title", equalTo("process application"))
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.location_name", equalTo("Taylor House"))
            .body("task.location", equalTo("765324"))
            .body("task.execution_type", equalTo("Case Management Task"))
            .body("task.jurisdiction", equalTo("WA"))
            .body("task.region", equalTo("1"))
            .body("task.case_type_id", equalTo("WaCaseType"))
            .body("task.case_id", equalTo(taskVariables.getCaseId()))
            .body("task.case_category", equalTo("Protection"))
            .body("task.case_name", equalTo("Bob Smith"))
            .body("task.auto_assigned", equalTo(false))
            .body("task.warnings", equalTo(false))
            .body("task.case_management_category", equalTo("Protection"))
            .body("task.work_type_id", equalTo("hearing_work"))
            .body("task.permissions.values", equalToObject(List.of("Read", "Refer", "Execute")))
            .body("task.description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                              + "trigger/decideAnApplication)"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void test() {

        Object body = "{\n"
                      + "    \"caseType\": {\n"
                      + "        \"caseAccessCategory\": \"categoryA,categoryC\",\n"
                      + "        \"appealType\": \"protection\",\n"
                      + "        \"appellantGivenNames\": \"Bob\",\n"
                      + "        \"appellantFamilyName\": \"Smith\",\n"
                      + "        \"TextField\": \"Some text field\"\n"
                      + "    },\n"
                      + "    \"taskAttributes\": {\n"
                      + "        \"caseTypeId\": \"a case type id\",\n"
                      + "        \"majorPriority\": 1,\n"
                      + "        \"jurisdiction\": \"a jurisdiction\",\n"
                      + "        \"regionName\": \"a region name\",\n"
                      + "        \"description\": \"a task description\",\n"
                      + "        \"roleCategory\": \"LEGAL_OPERATIONS\",\n"
                      + "        \"autoAssigned\": true,\n"
                      + "        \"title\": \"a task title\",\n"
                      + "        \"taskType\": \"processApplication\",\n"
                      + "        \"hasWarnings\": false,\n"
                      + "        \"caseId\": \"1655308607943415\",\n"
                      + "        \"state\": \"UNCONFIGURED\",\n"
                      + "        \"terminationReason\": \"a termination reason\",\n"
                      + "        \"locationName\": \"a location name\",\n"
                      + "        \"minorPriority\": 100,\n"
                      + "        \"created\": \"2022-06-15T16:57:19.784+01:00\",\n"
                      + "        \"dueDateTime\": \"2022-06-16T16:57:19.784+01:00\",\n"
                      + "        \"taskName\": \"task name\",\n"
                      + "        \"assignmentExpiry\": \"2022-06-16T16:57:19.784+01:00\",\n"
                      + "        \"location\": \"a location\",\n"
                      + "        \"assignee\": \"some assignee\",\n"
                      + "        \"caseCategory\": \"a case category\",\n"
                      + "        \"additionalProperties\": {\n"
                      + "            \"roleAssignmentId\": \"12345678\",\n"
                      + "            \"key8\": \"value8\",\n"
                      + "            \"key7\": \"value7\",\n"
                      + "            \"key6\": \"value6\",\n"
                      + "            \"key5\": \"value5\",\n"
                      + "            \"key4\": \"value4\",\n"
                      + "            \"key3\": \"value3\",\n"
                      + "            \"key2\": \"value2\",\n"
                      + "            \"key1\": \"value1\"\n"
                      + "        },\n"
                      + "        \"region\": \"a region\",\n"
                      + "        \"taskId\": \"c8d67bb0-ecc3-11ec-9bd1-0242ac11000c\"\n"
                      + "    }\n"
                      + "}";

        Response result = restApiActions.post(
            "http://camunda-local-bpm/engine-rest/decision-definition/key/wa-task-configuration-wa-wacasetype/tenant-id/wa/evaluate",
            body,
            caseworkerCredentials.getHeaders()
        );

        List<Map<String, Object>> list = result.then()
            .extract()
            .body()
            //.asString()
            .jsonPath()
            //.getMap("");
            .getList("");
        result.prettyPrint();

    }

    @Test
    public void should_return_a_200_with_task_and_correct_properties_V2() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask2(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.get(
            "/task/v2/{task-id}",
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.task_system", equalTo("SELF"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.task_title", equalTo("process application"))
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.location_name", equalTo("Taylor House"))
            .body("task.location", equalTo("765324"))
            .body("task.execution_type", equalTo("Case Management Task"))
            .body("task.jurisdiction", equalTo("WA"))
            .body("task.region", equalTo("1"))
            .body("task.case_type_id", equalTo("WaCaseType"))
            .body("task.case_id", equalTo(taskVariables.getCaseId()))
            .body("task.case_category", equalTo("Protection"))
            .body("task.case_name", equalTo("Bob Smith"))
            .body("task.auto_assigned", equalTo(false))
            .body("task.warnings", equalTo(false))
            .body("task.case_management_category", equalTo("Protection"))
            .body("task.work_type_id", equalTo("hearing_work"))
            .body("task.permissions.values", equalToObject(List.of("Read", "Refer", "Execute")))
            .body("task.description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                              + "trigger/decideAnApplication)"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }


    @Test
    public void should_return_403_when_user_grant_type_standard_and_excluded() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.task_system", equalTo("SELF"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.task_title", equalTo("process application"))
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue())
            .body("task.location_name", equalTo("Taylor House"))
            .body("task.location", equalTo("765324"))
            .body("task.execution_type", equalTo("Case Management Task"))
            .body("task.jurisdiction", equalTo("WA"))
            .body("task.region", equalTo("1"))
            .body("task.case_type_id", equalTo("WaCaseType"))
            .body("task.case_id", equalTo(taskVariables.getCaseId()))
            .body("task.case_category", equalTo("Protection"))
            .body("task.case_name", equalTo("Bob Smith"))
            .body("task.auto_assigned", equalTo(false))
            .body("task.warnings", equalTo(false))
            .body("task.case_management_category", equalTo("Protection"))
            .body("task.work_type_id", equalTo("hearing_work"))
            .body("task.permissions.values", equalToObject(List.of("Read", "Refer", "Execute")))
            .body("task.description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                              + "trigger/decideAnApplication)"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        //add excluded grantType
        common.setupExcludedAccessJudiciary(caseworkerCredentials.getHeaders(), taskVariables.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_replace_additional_properties_in_configuration_dmn_and_return_task_with_sent_properties() {

        TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        String roleAssignmentId = UUID.randomUUID().toString();
        common.setupHearingPanelJudgeForSpecificAccess(caseworkerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);

        Map<String, String> additionalProperties = Map.of(
            "roleAssignmentId", roleAssignmentId,
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4",
            "key5", "value5",
            "key6", "value6",
            "key7", "value7",
            "key8", "value8"
        );

        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "reviewSpecificAccessRequestJudiciary", "task name", "task title",
            additionalProperties);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.additional_properties", equalToObject(Map.of(
                "roleAssignmentId", roleAssignmentId
            )));

        common.cleanUpTask(taskId);
    }

}
