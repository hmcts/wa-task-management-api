package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class PostTaskAssignByIdControllerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/assign";

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerBAuthorizationOnly());
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo(String.format(
                LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK,
                nonExistentTaskId
            )));
    }

    private String getAssigneeId(Header caseworkerBAuthorizationOnly) {
        return authorizationHeadersProvider.getUserInfo(caseworkerBAuthorizationOnly.getValue()).getUid();
    }

    @Test
    public void should_return_a_204_when_assigning_a_task_by_id() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerBAuthorizationOnly());
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            task.get("taskId"),
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(task.get("taskId"), "taskState", "assigned");
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        Map<String, String> task = common.setupTaskAndRetrieveIds(Common.TRIBUNAL_CASEWORKER_PERMISSIONS);

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerBAuthorizationOnly());
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            task.get("taskId"),
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));
    }

    @Test
    public void should_return_a_403_when_the_assigner_does_not_have_manage_permission() {
        String no_manage_permission = "Read,Refer,Own,Cancel";
        Map<String, String> task = common.setupTaskAndRetrieveIds(no_manage_permission);

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerAAuthorizationOnly());
        String taskId = task.get("taskId");
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message",
                  equalTo("User did not have sufficient permissions to assign task with id: " + taskId));
    }

    @Test
    public void should_return_a_403_when_the_assignee_does_not_have_execute_or_own_permissions() {
        String no_own_permission = "Read,Refer,Manage,Cancel";
        Map<String, String> task = common.setupTaskAndRetrieveIds(no_own_permission);

        String assigneeId = getAssigneeId(authorizationHeadersProvider.getCaseworkerAAuthorizationOnly());
        String taskId = task.get("taskId");
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new AssigneeRequest(assigneeId),
            APPLICATION_JSON_VALUE,
            APPLICATION_JSON_VALUE,
            authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(LocalDateTime.now()
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.FORBIDDEN.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.FORBIDDEN.value()))
            .body("message",
                  equalTo("User did not have sufficient permissions to assign task with id: " + taskId));
    }

}

