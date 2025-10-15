package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasItems;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils.WA_CASE_WORKER;

public class GetTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    TestAuthenticationCredentials waCaseworkerCredentials;

    @Before
    public void setUp() {
        waCaseworkerCredentials = taskFunctionalTestsUserUtils.getTestUser(WA_CASE_WORKER);
    }

    @After
    public void cleanUp() {
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(waCaseworkerCredentials.getHeaders());
    }

    @Test
    public void should_return_a_200_with_task_and_correct_properties() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
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
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"))
            .body("task.description", equalTo(
                "[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                  + "trigger/decideAnApplication)"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )))
            .body("task.next_hearing_id", equalTo("next-hearing-id"))
            .body("task.next_hearing_date", notNullValue());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_403_when_user_grant_type_standard_and_excluded() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
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
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"))
            .body("task.description", equalTo(
                "[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                  + "trigger/decideAnApplication)"))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3",
                "key4", "value4"
            )))
            .body("task.next_hearing_id", equalTo("next-hearing-id"))
            .body("task.next_hearing_date", notNullValue());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        //add excluded grantType
        taskFunctionalTestsApiUtils.getCommon().setupExcludedAccessJudiciary(
            waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("type", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TYPE))
            .body("title", equalTo(ROLE_ASSIGNMENT_VERIFICATION_TITLE))
            .body("status", equalTo(403))
            .body("detail", equalTo(ROLE_ASSIGNMENT_VERIFICATION_DETAIL_REQUEST_FAILED));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_replace_additional_properties_in_configuration_dmn_and_return_task_with_sent_properties() {
        String roleAssignmentId = UUID.randomUUID().toString();
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

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithAdditionalPropertiesAndRetrieveIds(additionalProperties,
            "requests/ccd/wa_case_data.json",
            "reviewSpecificAccessRequestJudiciary"
        );
        String taskId = taskVariables.getTaskId();

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            waCaseworkerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );

        initiateTask(taskVariables, waCaseworkerCredentials.getHeaders(), additionalProperties);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.additional_properties", equalToObject(Map.of(
                "roleAssignmentId", roleAssignmentId
            )));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_task_id_in_description_when_creating_standalone_task() {
        String taskType = "reviewSpecificAccessRequestLegalOps";
        String taskName = "review specific access request legal ops";
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWAStandaloneTaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            taskType,
            taskName
        );
        String taskId = taskVariables.getTaskId();
        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId))
            .body("task.name", equalTo(taskName))
            .body("task.type", equalTo(taskType))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.task_system", equalTo("SELF"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.task_title", equalTo(taskName))
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
            .body("task.work_type_id", equalTo("access_requests"))
            .body("task.permissions.values", equalToObject(List.of("Read", "Own", "Manage", "Claim")))
            .body("task.description", equalTo(taskId))
            .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
            .body("task.additional_properties", equalToObject(Map.of(
                "roleAssignmentId", "roleAssignmentId"
            )))
            .body("task.next_hearing_id", equalTo("next-hearing-id"))
            .body("task.next_hearing_date", notNullValue());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_with_task_warnings() {

        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon()
            .setupWATaskWithWarningsAndRetrieveIds("processApplication", "process application");
        String taskId = taskVariables.getTaskId();

        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignmentWithCustomAttributes(
            waCaseworkerCredentials.getHeaders(),
            Map.of(
                "primaryLocation", "765324",
                "jurisdiction", "WA"
            )
        );

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId,
            waCaseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.warnings", is(true))
            .body("task.permissions.values.size()", equalTo(6))
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own", "Manage"));

        final List<Map<String, String>> actualWarnings = result.jsonPath().getList(
            "task.warning_list.values");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("warningCode", "Code1", "warningText", "Text1"),
            Map.of("warningCode", "Code2", "warningText", "Text2")
        );
        Assertions.assertEquals(expectedWarnings, actualWarnings);

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_and_add_permission_for_specific_role() {

        TestVariables taskVariables1 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );
        TestVariables taskVariables2 = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "requests/ccd/wa_case_data.json",
            "processApplication",
            "process application"
        );

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            waCaseworkerCredentials.getHeaders());

        initiateTask(taskVariables1);
        initiateTask(taskVariables2);

        String taskId1 = taskVariables1.getTaskId();
        String taskId2 = taskVariables2.getTaskId();

        Response result1 = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );

        Response result2 = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId1))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"));

        result2.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId2))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"));

        //add a case permission
        taskFunctionalTestsApiUtils.getCommon().setupFtpaJudgeForCaseAccess(
            waCaseworkerCredentials.getHeaders(), taskVariables1.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);

        result1 = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId1,
            waCaseworkerCredentials.getHeaders()
        );

        result2 = taskFunctionalTestsApiUtils.getRestApiActions().get(
            ENDPOINT_BEING_TESTED,
            taskId2,
            waCaseworkerCredentials.getHeaders()
        );

        result1.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId1))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"));

        result2.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("task.id", equalTo(taskId2))
            .body("task.name", equalTo("process application"))
            .body("task.type", equalTo("processApplication"))
            .body("task.permissions.values", hasItems("Read", "CompleteOwn", "CancelOwn", "Claim", "Own"));

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId1);
        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId2);
    }

}
