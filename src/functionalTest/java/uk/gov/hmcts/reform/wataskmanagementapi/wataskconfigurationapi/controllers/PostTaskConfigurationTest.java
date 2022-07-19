package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.request.ConfigureTaskRequest;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.utils.CreateTaskMessageBuilder.createMessageForTask;

@Slf4j
public class PostTaskConfigurationTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task-configuration/{task-id}/configuration";

    private String taskId;
    private String caseId;
    private CreateTaskMessage createTaskMessage;
    private TestAuthenticationCredentials caseworkerCredentials;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-");
    }

    @After
    public void cleanUp() {
        common.cleanUpTask(taskId);
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }

    @Test
    public void given_task_is_configured_then_expect_task_is_auto_assigned() throws Exception {
        caseId = given.iCreateACcdCase();

        createTaskMessage = createMessageForTask("arrangeOfflinePayment", caseId).build();
        taskId = createTask(createTaskMessage);
        log.info("task found [{}]", taskId);

        Map<String, Object> requiredProcessVariables = Map.of(
            TASK_ID.value(), "arrangeOfflinePayment",
            CASE_ID.value(), caseId,
            TASK_NAME.value(), "task name"
        );

        log.info("Creating roles...");
        common.setupRestrictedRoleAssignment(caseId, caseworkerCredentials.getHeaders());

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new ConfigureTaskRequest(requiredProcessVariables),
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.prettyPeek();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(taskId))
            .body("case_id", equalTo(caseId))
            .body("assignee", notNullValue())
            .body("configuration_variables", notNullValue())
            .body("configuration_variables.taskType", equalTo("arrangeOfflinePayment"))
            .body("configuration_variables.jurisdiction", equalTo("IA"))
            .body("configuration_variables.caseTypeId", equalTo("Asylum"))
            .body("configuration_variables.taskState", equalTo("assigned"))
            .body("configuration_variables.executionType", equalTo("Case Management Task"))
            .body("configuration_variables.caseId", equalTo(caseId))
            .body("configuration_variables.securityClassification", equalTo("PUBLIC"))
            .body("configuration_variables.autoAssigned", equalTo(true))
            .body("configuration_variables.taskSystem", equalTo("SELF"));
    }

    @Test
    public void should_return_task_configuration_then_expect_task_is_unassigned() throws Exception {
        caseId = given.iCreateACcdCase();
        createTaskMessage = createMessageForTask("wa-task-configuration-api-task", UUID.randomUUID().toString())
            .withCaseId(caseId)
            .build();
        taskId = createTask(createTaskMessage);
        log.info("task found [{}]", taskId);

        Map<String, Object> requiredProcessVariables = Map.of(
            TASK_ID.value(), "followUpOverdueReasonsForAppeal",
            CASE_ID.value(), caseId,
            TASK_NAME.value(), "task name"
        );

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new ConfigureTaskRequest(requiredProcessVariables),
            authorizationProvider.getServiceAuthorizationHeader()
        );

        result.prettyPeek();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("task_id", equalTo(taskId))
            .body("case_id", equalTo(caseId))
            .body("assignee", nullValue())
            .body("configuration_variables", notNullValue())
            .body("configuration_variables.taskType", equalTo("followUpOverdueReasonsForAppeal"))
            .body("configuration_variables.jurisdiction", equalTo("IA"))
            .body("configuration_variables.caseTypeId", equalTo("Asylum"))
            .body("configuration_variables.taskState", equalTo("unassigned"))
            .body("configuration_variables.executionType", equalTo("Case Management Task"))
            .body("configuration_variables.caseId", equalTo(caseId))
            .body("configuration_variables.securityClassification", equalTo("PUBLIC"))
            .body("configuration_variables.autoAssigned", equalTo(false))
            .body("configuration_variables.tribunal-caseworker", equalTo("Read,Refer,Own,Manage,Cancel"))
            .body("configuration_variables.senior-tribunal-caseworker",
                equalTo("Read,Refer,Own,Manage,Cancel"))
            .body("configuration_variables.taskSystem", equalTo("SELF"));
    }

}
