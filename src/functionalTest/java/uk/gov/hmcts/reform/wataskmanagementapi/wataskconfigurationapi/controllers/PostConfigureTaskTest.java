package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CreateTaskMessage;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.utils.CreateTaskMessageBuilder.createBasicMessageForTask;

@Slf4j
public class PostConfigureTaskTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "task-configuration/{task-id}";

    private String taskId;
    private CreateTaskMessage createTaskMessage;
    private String caseId;

    @After
    public void cleanUp() {
        super.cleanUp(taskId);
    }

    @Test
    public void given_configure_task_then_expect_task_state_is_assigned() throws Exception {
        caseId = createCcdCase();

        String taskTypeId = "followUpOverdueReasonsForAppeal";
        createTaskMessage = createBasicMessageForTask(taskTypeId, caseId).build();
        this.taskId = createTask(createTaskMessage);
        log.info("task found [{}]", this.taskId);

        log.info("Creating roles...");
        roleAssignmentHelper.setRoleAssignments(caseId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            this.taskId,
            new Headers(authorizationHeadersProvider.getServiceAuthorizationHeader())
        );
        result.prettyPeek();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE);

        Response camundaResult = camundaApiActions.get(
            "/task/{task-id}/variables",
            this.taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        camundaResult.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("taskType.value", is(taskTypeId))
            .body("workType.value", is("decision_making_work"))
            .body("caseName.value", is("Bob Smith"))
            .body("appealType.value", is("Protection"))
            .body("region.value", is("1"))
            .body("location.value", is("765324"))
            .body("locationName.value", is("Taylor House"))
            .body("taskState.value", is("assigned"))
            .body("caseId.value", is(createTaskMessage.getCaseId()))
            .body("securityClassification.value", is("PUBLIC"))
            .body("jurisdiction.value", is("IA"))
            .body("caseTypeId.value", is("Asylum"))
            .body("title.value", is("task name"))
            .body("hasWarnings.value", is(false))
            .body("tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"))
            .body("senior-tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"));
    }

    @Test
    public void given_configure_task_then_expect_task_state_is_assigned_and_has_work_type() throws Exception {
        caseId = createCcdCase();

        String taskTypeId = "followUpOverdueReasonsForAppeal";
        createTaskMessage = createBasicMessageForTask(taskTypeId, caseId).build();
        this.taskId = createTask(createTaskMessage);
        log.info("task found [{}]", this.taskId);

        log.info("Creating roles...");
        roleAssignmentHelper.setRoleAssignments(caseId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            this.taskId,
            new Headers(authorizationHeadersProvider.getServiceAuthorizationHeader())
        );
        result.prettyPeek();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE);

        Response camundaResult = camundaApiActions.get(
            "/task/{task-id}/variables",
            this.taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        camundaResult.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("taskType.value", is(taskTypeId))
            .body("workType.value", is("decision_making_work"))
            .body("caseName.value", is("Bob Smith"))
            .body("appealType.value", is("Protection"))
            .body("region.value", is("1"))
            .body("location.value", is("765324"))
            .body("locationName.value", is("Taylor House"))
            .body("taskState.value", is("assigned"))
            .body("caseId.value", is(createTaskMessage.getCaseId()))
            .body("securityClassification.value", is("PUBLIC"))
            .body("jurisdiction.value", is("IA"))
            .body("caseTypeId.value", is("Asylum"))
            .body("title.value", is("task name"))
            .body("hasWarnings.value", is(false))
            .body("tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"))
            .body("senior-tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"))
            .body("workType.value", is("decision_making_work"));
    }

    @Test
    public void given_configure_task_then_expect_task_state_is_unassigned() throws IOException {
        caseId = createCcdCase();
        createTaskMessage = createBasicMessageForTask("wa-task-configuration-api-task", UUID.randomUUID().toString())
            .withCaseId(caseId)
            .build();
        taskId = createTask(createTaskMessage);
        log.info("task found [{}]", taskId);

        Response result = restApiActions.post(
            ENDPOINT_BEING_TESTED,
            taskId,
            new Headers(authorizationHeadersProvider.getServiceAuthorizationHeader())
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE);

        Response camundaResult = camundaApiActions.get(
            "/task/{task-id}/variables",
            taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        camundaResult.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("taskType.value", is("wa-task-configuration-api-task"))
            .body("caseName.value", is("Bob Smith"))
            .body("appealType.value", is("Protection"))
            .body("region.value", is("1"))
            .body("location.value", is("765324"))
            .body("locationName.value", is("Taylor House"))
            .body("taskState.value", is("unassigned"))
            .body("caseId.value", is(createTaskMessage.getCaseId()))
            .body("securityClassification.value", is("PUBLIC"))
            .body("jurisdiction.value", is("IA"))
            .body("caseTypeId.value", is("Asylum"))
            .body("title.value", is("task name"))
            .body("tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"))
            .body("senior-tribunal-caseworker.value", is("Read,Refer,Own,Manage,Cancel"));
    }

}
