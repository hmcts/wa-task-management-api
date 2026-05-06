package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.BASE_CASE_WORDER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_GET_ENDPOINT;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_INITIATION_PUSH_ENDPOINT;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public class PostTaskInitiatePushByIdControllerTest {

    @Autowired
    private TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    private TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    private AuthorizationProvider authorizationProvider;

    @Test
    public void should_initiate_a_task_from_camunda_push_payload() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "processApplication", "Process Application"
            );

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put(CamundaVariableDefinition.TASK_TYPE.value(), new CamundaVariable(taskVariables.getTaskType(), "String"));
        variables.put(CamundaVariableDefinition.TASK_ID.value(), new CamundaVariable(taskVariables.getTaskType(), "String"));
        variables.put(CamundaVariableDefinition.CASE_ID.value(), new CamundaVariable(taskVariables.getCaseId(), "String"));
        variables.put(CamundaVariableDefinition.SECURITY_CLASSIFICATION.value(), new CamundaVariable("PUBLIC", "String"));
        variables.put(CamundaVariableDefinition.HAS_WARNINGS.value(), new CamundaVariable(false, "Boolean"));
        variables.put(CamundaVariableDefinition.CFT_TASK_STATE.value(), new CamundaVariable("unconfigured", "String"));
        variables.put("__processCategory__Protection", new CamundaVariable(true, "Boolean"));

        ZonedDateTime created = ZonedDateTime.now();
        ZonedDateTime due = created.plusDays(10);
        CamundaTaskInitiationRequest request = new CamundaTaskInitiationRequest(
            taskVariables.getTaskName(),
            null,
            created,
            due,
            null,
            taskVariables.getProcessInstanceId(),
            variables
        );

        Response pushResponse = taskFunctionalTestsApiUtils.getRestApiActions().post(
            TASK_INITIATION_PUSH_ENDPOINT,
            taskVariables.getTaskId(),
            request,
            authorizationProvider.getServiceAuthorizationHeadersOnly()
        );

        pushResponse.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .body("task_id", equalTo(taskVariables.getTaskId()))
            .body("state", equalTo("UNASSIGNED"));

        Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
            TASK_GET_ENDPOINT,
            taskVariables.getTaskId(),
            taskFunctionalTestsUserUtils.getTestUser(BASE_CASE_WORDER).getHeaders()
        );

        response.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .body("task.id", equalTo(taskVariables.getTaskId()))
            .body("task.case_id", equalTo(taskVariables.getCaseId()))
            .body("task.name", equalTo("Process Application"))
            .body("task.task_state", equalTo("unassigned"))
            .body("task.created_date", notNullValue())
            .body("task.due_date", notNullValue());

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskVariables.getTaskId());
    }
}
