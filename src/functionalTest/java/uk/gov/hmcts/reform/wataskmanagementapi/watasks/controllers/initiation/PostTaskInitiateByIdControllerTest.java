package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers.initiation;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootTasksMapTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.Jurisdiction;

import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;

public class PostTaskInitiateByIdControllerTest extends SpringBootTasksMapTest {

    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewWaTribunalCaseworker("wa-ft-test-r2");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_201_when_initiating_a_process_application_task_by_id() {
        TestVariables taskVariables =
            common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
                                             "processApplication", "Process Application");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("Process Application"))
                .body("task.type", equalTo("processApplication"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("Process Application"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                 + "trigger/decideAnApplication)"))
                .body("task.permissions.values.size()", equalTo(3))
                .body("task.permissions.values", hasItems("Read", "Refer", "Execute"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))).body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body("task.priority_date", equalTo("2022-12-07T13:00:00Z"));
        };

        initiateTaskMap(taskVariables, Jurisdiction.WA, assertConsumer);

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_201_when_initiating_a_specific_access_task_by_id() {
        TestVariables taskVariables =
            common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_fixed_hearing_date.json",
                                             "reviewSpecificAccessRequestJudiciary",
                                             "additionalProperties_roleAssignmentId");
        String taskId = taskVariables.getTaskId();
        common.setupCFTJudicialOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(),
                                                            taskVariables.getCaseId(),
                                                            WA_JURISDICTION, WA_CASE_TYPE);

        //Note: this is the TaskResource.class
        Consumer<Response> assertConsumer = (result) -> {
            result.prettyPrint();

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("additionalProperties_roleAssignmentId"))
                .body("task.type", equalTo("reviewSpecificAccessRequestJudiciary"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("additionalProperties_roleAssignmentId"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.permissions.values.size()", equalTo(3))
                .body("task.permissions.values", hasItems("Read", "Refer", "Own"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "roleAssignmentId", "roleAssignmentId")))
                .body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(1000))
                .body("task.priority_date", equalTo("2022-12-07T13:00:00Z"));
        };

        initiateTaskMap(taskVariables, caseworkerCredentials.getHeaders(), assertConsumer);

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_priorty_date_when_initiating_a_task_without_hearing_date() {
        TestVariables taskVariables
            = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data_no_hearing_date.json",
                                               "processApplication",
                                               "process Application");
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignmentForWA(caseworkerCredentials.getHeaders());

        Consumer<Response> assertConsumer = (result) -> {
            //Note: this is the TaskResource.class
            result.prettyPrint();

            //TODO: uncomment this once priority_date format is fixed in RWA-1779 ticket
            //ZonedDateTime dueDate = ZonedDateTime.parse(result.jsonPath().get("task.due_date"),
            //                                            ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
            //String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

            //OffsetDateTime priorityDate = OffsetDateTime.parse(result.jsonPath().get("task.priority_date"));
            //String formattedPriorityDate = CAMUNDA_DATA_TIME_FORMATTER.format(priorityDate);
            //Assert.assertEquals(formattedDueDate, formattedPriorityDate);

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("task.id", equalTo(taskId))
                .body("task.name", equalTo("process Application"))
                .body("task.type", equalTo("processApplication"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.task_system", equalTo("SELF"))
                .body("task.security_classification", equalTo("PUBLIC"))
                .body("task.task_title", equalTo("process Application"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue())
                .body("task.auto_assigned", equalTo(false))
                .body("task.warnings", equalTo(false))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.case_type_id", equalTo("WaCaseType"))
                .body("task.case_category", equalTo("Protection"))
                .body("task.jurisdiction", equalTo("WA"))
                .body("task.region", equalTo("1"))
                .body("task.location", equalTo("765324"))
                .body("task.location_name", equalTo("Taylor House"))
                .body("task.execution_type", equalTo("Case Management Task"))
                .body("task.work_type_id", equalTo("hearing_work"))
                .body("task.work_type_label", equalTo("Hearing work"))
                .body("task.role_category", equalTo("LEGAL_OPERATIONS"))
                .body("task.description", equalTo("[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/"
                                                 + "trigger/decideAnApplication)"))
                .body("task.permissions.values.size()", equalTo(3))
                .body("task.permissions.values", hasItems("Read", "Refer", "Execute"))
                .body("task.additional_properties", equalToObject(Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "key3", "value3",
                    "key4", "value4"
                ))).body("task.minor_priority", equalTo(500))
                .body("task.major_priority", equalTo(5000));
        };

        initiateTaskMap(taskVariables, Jurisdiction.WA, assertConsumer);

        common.cleanUpTask(taskId);
    }
}
