package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.util.List;
import java.util.UUID;

public class PostTaskReconfigureControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/operation";
    private TestAuthenticationCredentials caseworkerCredentials;
    private String assigneeId;

    @Before
    public void setUp() {
        caseworkerCredentials = authorizationProvider.getNewTribunalCaseworker("wa-ft-test-r2-");
        assigneeId = getAssigneeId(caseworkerCredentials.getHeaders());
    }

    @After
    public void cleanUp() {
        common.clearAllRoleAssignments(caseworkerCredentials.getHeaders());
        authorizationProvider.deleteAccount(caseworkerCredentials.getAccount().getUsername());
    }


    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_assigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "followUpOverdueReasonsForAppeal", "follow Up Overdue Reasons For Appeal", "A test task");

        String assignEndpoint = "task/{task-id}/assign";
        Response result = restApiActions.post(
            assignEndpoint,
            taskId,
            new AssignTaskRequest(assigneeId),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        assertions.taskVariableWasUpdated(taskVariables.getProcessInstanceId(), "taskState", "assigned");

        assertions.taskStateWasUpdatedinDatabase(taskId, "assigned", caseworkerCredentials.getHeaders());
        assertions.taskFieldWasUpdatedInDatabase(taskId, "assignee", assigneeId, caseworkerCredentials.getHeaders());

        result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }


    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_unassigned() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupCFTOrganisationalRoleAssignment(caseworkerCredentials.getHeaders(), "IA", "Asylum");
        initiateTask(caseworkerCredentials.getHeaders(), taskVariables,
            "followUpOverdueReasonsForAppeal", "follow Up Overdue Reasons For Appeal", "A test task");

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseworkerCredentials.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());

        common.cleanUpTask(taskId);
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName, String caseId) {
        TaskOperation operation = new TaskOperation(operationName, UUID.randomUUID().toString());
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private List<TaskFilter> taskFilters(String caseId) {
        TaskFilter filter = new TaskFilter("case_id", List.of(caseId), TaskFilterOperator.IN);
        return List.of(filter);
    }


}

