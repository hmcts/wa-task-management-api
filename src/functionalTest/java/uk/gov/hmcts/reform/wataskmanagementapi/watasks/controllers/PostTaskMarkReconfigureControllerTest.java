package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;

public class PostTaskMarkReconfigureControllerTest extends SpringBootFunctionalBaseTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    TestAuthenticationCredentials assignerCredentials;
    TestAuthenticationCredentials assigneeCredentials;

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    private String taskId;

    @Before
    public void setUp() {
        assignerCredentials = taskFunctionalTestsUserUtils.getTestUser(TaskFunctionalTestsUserUtils.ASSIGNER);
        assigneeCredentials = taskFunctionalTestsUserUtils.getTestUser(TaskFunctionalTestsUserUtils.ASSIGNEE);
    }

    @After
    public void cleanUp() {
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assigneeCredentials.getHeaders());
    }

    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_assigned_for_WA() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );


        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        initiateTask(taskVariables);

        taskFunctionalTestsApiUtils.getCommon().setupWAOrganisationalRoleAssignment(
            assigneeCredentials.getHeaders(), "tribunal-caseworker");

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            assigneeCredentials.getHeaders()));

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    @Test
    public void should_return_a_200_after_tasks_are_marked_for_reconfigure_when_task_status_is_unassigned_for_WA() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(
            assigneeCredentials.getHeaders(), WA_JURISDICTION, WA_CASE_TYPE);
        initiateTask(taskVariables);

        taskId = taskVariables.getTaskId();

        //before mark to reconfigure
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
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
            .body("task.reconfigure_request_time", nullValue())
            .body("task.last_reconfiguration_time", nullValue());

        //mark to reconfigure
        result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            assigneeCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        //after mark to reconfigure
        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
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

        taskFunctionalTestsApiUtils.getCommon().cleanUpTask(taskId);
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType operationName, String caseId) {
        TaskOperation operation = TaskOperation.builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(2)
            .retryWindowHours(120)
            .build();
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private List<TaskFilter<?>> taskFilters(String caseId) {
        MarkTaskToReconfigureTaskFilter filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId) {

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            "task/{task-id}/assign",
            taskVariables.getTaskId(),
            new AssignTaskRequest(assigneeId),
            assignerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        taskFunctionalTestsApiUtils.getCommon().setupCFTOrganisationalRoleAssignment(assignerCredentials.getHeaders(),
                                                    WA_JURISDICTION, WA_CASE_TYPE
        );

        taskFunctionalTestsApiUtils.getAssertions().taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(), "taskState", "assigned");
        taskFunctionalTestsApiUtils.getAssertions().taskStateWasUpdatedInDatabase(
            taskVariables.getTaskId(), "assigned", assignerCredentials.getHeaders());
        taskFunctionalTestsApiUtils.getAssertions()
            .taskFieldWasUpdatedInDatabase(
                taskVariables.getTaskId(), "assignee", assigneeId, assignerCredentials.getHeaders()
            );
    }

}

