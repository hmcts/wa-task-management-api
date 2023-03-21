package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteCaseTasksAction;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;

public class DeleteTasksByCaseIdControllerTest extends SpringBootFunctionalBaseTest {


    private static final String ENDPOINT_BEING_TESTED = "task/delete";
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
    public void should_return_201_when_task_deleted() {
        final TestVariables taskVariables = common.setupWATaskAndRetrieveIds();
        initiateTask(taskVariables);

        final DeleteTasksRequest deleteTasksRequest = new DeleteTasksRequest(new DeleteCaseTasksAction(
                taskVariables.getCaseId()));

        final Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                deleteTasksRequest,
                caseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void should_return_400_when_when_case_id_incorrect() {
        final DeleteTasksRequest deleteTasksRequest = new DeleteTasksRequest(new DeleteCaseTasksAction(
                "123"));

        final Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                deleteTasksRequest,
                caseworkerCredentials.getHeaders()
        );

        result.then().assertThat().statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    public void should_return_401_when_when_service_unauthorized() {
        final DeleteTasksRequest deleteTasksRequest = new DeleteTasksRequest(new DeleteCaseTasksAction(
                "1234567891234567"));

        final Response result = restApiActions.post(
                ENDPOINT_BEING_TESTED,
                deleteTasksRequest,
                new Header(AUTHORIZATION, "some_invalid_token")
        );

        result.then().assertThat().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

}
