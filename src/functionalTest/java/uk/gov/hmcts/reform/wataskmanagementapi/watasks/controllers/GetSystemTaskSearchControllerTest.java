package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public class GetSystemTaskSearchControllerTest {

    private static final String ENDPOINT_BEING_TESTED = "task/searchByCaseId";
    private static final String TEST_USER_EMAIL_PREFIX = "wa-task-search-functional";

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;
    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;
    @Autowired
    AuthorizationProvider authorizationProvider;

    @Test
    public void should_return_200_with_tasks_for_the_case_and_system_users_jurisdiction() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds();
        try {
            taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);
            TestAuthenticationCredentials searchUser =
                authorizationProvider.getNewWaTribunalCaseworkerWithStaticEmailAndStaticID(TEST_USER_EMAIL_PREFIX);

            Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
                ENDPOINT_BEING_TESTED + "?case_id=" + taskVariables.getCaseId(),
                searchUser.getHeaders()
            );

            result.then().assertThat()
                .statusCode(HttpStatus.OK.value())
                .contentType(APPLICATION_JSON_VALUE)
                .header("Cache-Control", "no-store")
                .body("tasks.id", contains(taskVariables.getTaskId()))
                .body("tasks.case_id", contains(taskVariables.getCaseId()))
                .body("tasks.jurisdiction", contains(WA_JURISDICTION))
                .body("tasks[0].permissions.values", empty());
        } finally {
            taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskVariables.getTaskId());
            authorizationProvider.deleteAccount(TEST_USER_EMAIL_PREFIX + "@fake.hmcts.net");
        }
    }
}
