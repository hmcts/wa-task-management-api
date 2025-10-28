package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.http.Header;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteCaseTasksAction;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_NO_ROLES;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class DeleteTasksByCaseIdControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    private static final String ENDPOINT_BEING_TESTED = "task/delete";

    private TestAuthenticationCredentials caseworkerWithNoRoles;

    @Before
    public void setUp() {
        caseworkerWithNoRoles = taskFunctionalTestsUserUtils.getTestUser(
            USER_WITH_NO_ROLES);
    }

    @Test
    public void should_return_201_when_task_deleted() {
        final TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds();
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        final DeleteTasksRequest deleteTasksRequest = new DeleteTasksRequest(new DeleteCaseTasksAction(
                taskVariables.getCaseId()));

        final Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                deleteTasksRequest,
                caseworkerWithNoRoles.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void should_return_401_when_when_service_unauthorized() {
        final DeleteTasksRequest deleteTasksRequest = new DeleteTasksRequest(new DeleteCaseTasksAction(
                "1234567891234567"));

        final Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
                ENDPOINT_BEING_TESTED,
                deleteTasksRequest,
                new Header(AUTHORIZATION, "some_invalid_token")
        );

        result.then().assertThat().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

}
