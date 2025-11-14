package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsApiUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsInitiationUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestsUserUtils;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.EMAIL_PREFIX_R3_5;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_CFT_ORG_ROLES;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.USER_WITH_TRIB_CASEWORKER_ROLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskFunctionalTestConstants.WA_JURISDICTION;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class PostTaskMarkReconfigureControllerTest {

    @Autowired
    TaskFunctionalTestsUserUtils taskFunctionalTestsUserUtils;

    @Autowired
    TaskFunctionalTestsApiUtils taskFunctionalTestsApiUtils;

    @Autowired
    TaskFunctionalTestsInitiationUtils taskFunctionalTestsInitiationUtils;

    @Autowired
    AuthorizationProvider authorizationProvider;

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    private String taskId;

    TestAuthenticationCredentials caseWorkerWithTribRole;
    TestAuthenticationCredentials caseWorkerWithCftOrgRoles;

    @Before
    public void setUp() {
        caseWorkerWithTribRole = taskFunctionalTestsUserUtils.getTestUser(USER_WITH_TRIB_CASEWORKER_ROLE);
        caseWorkerWithCftOrgRoles = taskFunctionalTestsUserUtils
            .getTestUser(USER_WITH_CFT_ORG_ROLES);
    }

    @Test
    public void should_return_a_204_after_tasks_are_marked_for_reconfigure_when_task_status_is_assigned_for_WA() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        TestAuthenticationCredentials assignerCredentials =
            authorizationProvider.getNewTribunalCaseworker(EMAIL_PREFIX_R3_5);

        taskFunctionalTestsApiUtils.getCommon().setupHearingPanelJudgeForSpecificAccess(
            assignerCredentials.getHeaders(), taskVariables.getCaseId(), WA_JURISDICTION, WA_CASE_TYPE
        );
        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        assignTaskAndValidate(taskVariables, taskFunctionalTestsUserUtils.getAssigneeId(
            caseWorkerWithTribRole.getHeaders()),assignerCredentials);

        Response result = taskFunctionalTestsApiUtils.getRestApiActions().post(
            ENDPOINT_BEING_TESTED,
            taskOperationRequest(TaskOperationType.MARK_TO_RECONFIGURE, taskVariables.getCaseId()),
            caseWorkerWithTribRole.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        taskId = taskVariables.getTaskId();

        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithTribRole.getHeaders()
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
        taskFunctionalTestsApiUtils.getCommon().clearAllRoleAssignments(assignerCredentials.getHeaders());
        authorizationProvider.deleteAccount(assignerCredentials.getAccount().getUsername());

    }

    @Test
    public void should_return_a_200_after_tasks_are_marked_for_reconfigure_when_task_status_is_unassigned_for_WA() {
        TestVariables taskVariables = taskFunctionalTestsApiUtils.getCommon().setupWATaskAndRetrieveIds(
            "processApplication",
            "Process Application"
        );

        taskFunctionalTestsInitiationUtils.initiateTask(taskVariables);

        taskId = taskVariables.getTaskId();

        //before mark to reconfigure
        Response result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
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
            caseWorkerWithCftOrgRoles.getHeaders()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value());

        //after mark to reconfigure
        result = taskFunctionalTestsApiUtils.getRestApiActions().get(
            "/task/{task-id}",
            taskId,
            caseWorkerWithCftOrgRoles.getHeaders()
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

    private void assignTaskAndValidate(TestVariables taskVariables, String assigneeId,
                                       TestAuthenticationCredentials assignerCredentials) {

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

