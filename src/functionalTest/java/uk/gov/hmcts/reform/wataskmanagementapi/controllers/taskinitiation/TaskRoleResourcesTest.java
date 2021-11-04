package uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TITLE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedCaseManagerTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedHearingCentreAdminTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedHearingJudgeTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedJudgeTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedNationalBusinessCentreTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedTaskSupervisorTaskRoleResource;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.taskinitiation.TaskRoleResourcesHelper.getExpectedTribunalCaseWorkerTaskRoleResource;

public class TaskRoleResourcesTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}";

    private Headers authenticationHeaders;
    private String taskId;
    private InitiateTaskRequest initiateTaskRequest;
    private TestVariables taskVariables;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("prefix");
        taskVariables = common.setupTaskAndRetrieveIds();
        taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        ZonedDateTime createdDate = ZonedDateTime.now();
        String formattedCreatedDate = CAMUNDA_DATA_TIME_FORMATTER.format(createdDate);
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        initiateTaskRequest = new InitiateTaskRequest(INITIATION, asList(
            new TaskAttribute(TASK_TYPE, ""),
            new TaskAttribute(TASK_NAME, "aTaskName"),
            new TaskAttribute(TASK_CASE_ID, taskVariables.getCaseId()),
            new TaskAttribute(TASK_TITLE, "A test task"),
            new TaskAttribute(TASK_CREATED, formattedCreatedDate),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
        ));
    }

    @Test
    public void given_task_is_initiated_when_task_type_is_empty_then_expect_rule_one() {
        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            initiateTaskRequest,
            authenticationHeaders
        );

        result.prettyPrint();
        result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_name", equalTo("aTaskName"))
            .body("task_type", equalTo(""))
            .body("task_role_resources[0].task_role_id", notNullValue())
            .body("task_role_resources[0].role_name", equalTo("task-supervisor"))
            .body("task_role_resources[0].read", equalTo(true))
            .body("task_role_resources[0].own", equalTo(false))
            .body("task_role_resources[0].execute", equalTo(false))
            .body("task_role_resources[0].cancel", equalTo(true))
            .body("task_role_resources[0].refer", equalTo(true))
            .body("task_role_resources[0].manage", equalTo(true))
            .body("task_role_resources[0].authorizations", equalTo(List.of("IA")))
            .body("task_role_resources[0].auto_assignable", equalTo(false))
            .body("task_role_resources[0].role_category", equalTo("LEGAL_OPERATIONS"));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void when_task_type_is_ReviewRespondentEvidence_then_expect_one_two_and_three_rules() {
        initiateTaskRequest.getTaskAttributes()
            .set(0, new TaskAttribute(TASK_TYPE, "reviewRespondentEvidence"));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            initiateTaskRequest,
            authenticationHeaders
        );

        result.prettyPrint();
        TaskResource actualTaskResource = result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_type", equalTo("reviewRespondentEvidence"))
            .extract()
            .as(TaskResource.class);

        Set<TaskRoleResource> actualTaskRoleResources = actualTaskResource.getTaskRoleResources();
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedCaseManagerTaskRoleResource()));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedTribunalCaseWorkerTaskRoleResource()));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedTaskSupervisorTaskRoleResource()));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void when_task_type_is_ArrangeOfflinePayment_then_expect_one_and_fourth_rules() {
        initiateTaskRequest.getTaskAttributes()
            .set(0, new TaskAttribute(TASK_TYPE, "arrangeOfflinePayment"));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            initiateTaskRequest,
            authenticationHeaders
        );

        result.prettyPrint();
        TaskResource actualTaskResource = result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_type", equalTo("arrangeOfflinePayment"))
            .extract()
            .as(TaskResource.class);

        Set<TaskRoleResource> actualTaskRoleResources = actualTaskResource.getTaskRoleResources();
        assertTrue(assertTaskRoleResources(
            actualTaskRoleResources,
            getExpectedNationalBusinessCentreTaskRoleResource()
        ));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedTaskSupervisorTaskRoleResource()));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void when_task_type_is_AllocateHearingJudge_then_expect_one_and_fifth_rules() {
        initiateTaskRequest.getTaskAttributes()
            .set(0, new TaskAttribute(TASK_TYPE, "allocateHearingJudge"));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            initiateTaskRequest,
            authenticationHeaders
        );

        result.prettyPrint();
        TaskResource actualTaskResource = result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_type", equalTo("allocateHearingJudge"))
            .extract()
            .as(TaskResource.class);

        Set<TaskRoleResource> actualTaskRoleResources = actualTaskResource.getTaskRoleResources();
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedHearingCentreAdminTaskRoleResource()));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedTaskSupervisorTaskRoleResource()));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }

    @Test
    public void when_task_type_is_ReviewHearingBundle_then_expect_one_sixth_and_seventh_rules() {
        initiateTaskRequest.getTaskAttributes()
            .set(0, new TaskAttribute(TASK_TYPE, "reviewHearingBundle"));

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            initiateTaskRequest,
            authenticationHeaders
        );

        result.prettyPrint();
        TaskResource actualTaskResource = result.then().assertThat()
            .statusCode(HttpStatus.CREATED.value())
            .and()
            .body("task_id", equalTo(taskId))
            .body("task_type", equalTo("reviewHearingBundle"))
            .extract()
            .as(TaskResource.class);

        Set<TaskRoleResource> actualTaskRoleResources = actualTaskResource.getTaskRoleResources();
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedHearingJudgeTaskRoleResource()));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedJudgeTaskRoleResource()));
        assertTrue(assertTaskRoleResources(actualTaskRoleResources, getExpectedTaskSupervisorTaskRoleResource()));

        assertions.taskVariableWasUpdated(
            taskVariables.getProcessInstanceId(),
            "cftTaskState",
            "unassigned"
        );

        common.cleanUpTask(taskId);
    }


    private boolean assertTaskRoleResources(Set<TaskRoleResource> actualTaskRoleResources,
                                            TaskRoleResource expectedTaskRoleResource) {
        return actualTaskRoleResources.stream()
                   .filter(a -> a.getRoleName().equals(expectedTaskRoleResource.getRoleName()))
                   .filter(a -> a.getRead().equals(expectedTaskRoleResource.getRead()))
                   .filter(a -> a.getOwn().equals(expectedTaskRoleResource.getOwn()))
                   .filter(a -> a.getExecute().equals(expectedTaskRoleResource.getExecute()))
                   .filter(a -> a.getManage().equals(expectedTaskRoleResource.getManage()))
                   .filter(a -> a.getCancel().equals(expectedTaskRoleResource.getCancel()))
                   .filter(a -> a.getRefer().equals(expectedTaskRoleResource.getRefer()))
                   .filter(a -> Arrays.equals(
                       a.getAuthorizations(),
                       expectedTaskRoleResource.getAuthorizations()
                   ))
                   .filter(a -> Objects.equals(
                       a.getAssignmentPriority(),
                       expectedTaskRoleResource.getAssignmentPriority()
                   ))
                   .filter(a -> a.getAutoAssignable().equals(expectedTaskRoleResource.getAutoAssignable()))
                   .filter(a -> a.getRoleCategory().equals(expectedTaskRoleResource.getRoleCategory()))
                   .count() == 1;
    }

}

