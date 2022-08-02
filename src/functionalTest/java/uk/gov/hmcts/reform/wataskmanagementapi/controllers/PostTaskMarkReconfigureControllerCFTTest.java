package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;

public class PostTaskMarkReconfigureControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    private TestAuthenticationCredentials assignerCredentials;
    private TestAuthenticationCredentials assigneeCredentials;
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        assigneeCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(assignerCredentials.getHeaders());
        common.clearAllRoleAssignments(assigneeCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());
        authorizationProvider.deleteAccount(assigneeCredentials.getAccount().getUsername());
    }

    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_assigned_for_WA() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");


        common.setupHearingPanelJudgeForSpecificAccess(assignerCredentials.getHeaders(),
            taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assignerCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);
        assignTaskAndValidate(taskVariables, getAssigneeId(assigneeCredentials.getHeaders()));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("assigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        common.cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_unassigned_for_WA() {
        TestVariables taskVariables = common.setupWATaskAndRetrieveIds("requests/ccd/wa_case_data.json");

        common.setupCaseManagerForSpecificAccess(assigneeCredentials.getHeaders(), taskVariables.getCaseId(),
            WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(assigneeCredentials.getHeaders(), taskVariables,
            "processApplication", "process application", "process task");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskId = taskVariables.getTaskId();

        result = restApiActions.get(
            "/task/{task-id}",
            taskId,
            assigneeCredentials.getHeaders()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .and().body("task.id", equalTo(taskId))
            .body("task.task_state", is("unassigned"))
            .body("task.reconfigure_request_time", notNullValue())
            .body("task.last_reconfiguration_time", nullValue());

        common.cleanUpTask(taskId);
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName, String caseId) {
        TaskOperation operation = new TaskOperation(operationName, UUID.randomUUID().toString(), 2, 120);
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private List<TaskFilter<?>> taskFilters(String caseId) {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId) {

        Response result = restApiActions.post(
            "task/{task-id}/assign",
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(),
            WA_JURISDICTION, WA_CASE_TYPE);

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");
        assertions.taskStateWasUpdatedInDatabase(taskVariables.getTaskId(), "assigned",
            assignerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskVariables.getTaskId(), "assignee",
            assigneeId, assignerCredentials.getHeaders());
    }

}

