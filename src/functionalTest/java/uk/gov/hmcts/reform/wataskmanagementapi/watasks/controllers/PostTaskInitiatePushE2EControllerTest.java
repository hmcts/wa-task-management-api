package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AwaitilityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.BASE_CASE_WORDER;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.TASK_GET_ENDPOINT;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Import(AwaitilityTestConfig.class)
public class PostTaskInitiatePushE2EControllerTest {

    @Autowired
    private TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    private TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    private TestAuthenticationCredentials baseCaseWorker;

    @Before
    public void setUp() {
        baseCaseWorker = taskFunctionalTestsUserUtils.getTestUser(BASE_CASE_WORDER);
    }

    @Disabled
    @Test
    public void should_initiate_a_task_via_camunda_bpm_push_flow() {
        TestVariables taskVariables =
            taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
                "requests/ccd/wa_case_data_fixed_hearing_date.json",
                "processApplication", "Process Application"
            );

        await().untilAsserted(() -> {
            Response response = taskFunctionalTestsApiUtils.getRestApiActions().get(
                TASK_GET_ENDPOINT,
                taskVariables.getTaskId(),
                baseCaseWorker.getHeaders()
            );

            response.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .body("task.id", equalTo(taskVariables.getTaskId()))
                .body("task.case_id", equalTo(taskVariables.getCaseId()))
                .body("task.name", equalTo("Process Application"))
                .body("task.task_state", equalTo("unassigned"))
                .body("task.created_date", notNullValue())
                .body("task.due_date", notNullValue());
        });

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskVariables.getTaskId());
    }
}
