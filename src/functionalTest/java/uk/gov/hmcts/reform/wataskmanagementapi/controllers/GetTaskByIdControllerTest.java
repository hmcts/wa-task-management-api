package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;

public class GetTaskByIdControllerTest extends SpringBootFunctionalBaseTest {

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.get(
            "task/{task-id}",
            nonExistentTaskId,
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", is(notNullValue()))
            .body("error", equalTo(HttpStatus.NOT_FOUND.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("message", equalTo("There was a problem fetching the task with id: " + nonExistentTaskId));
    }

    @Test
    public void should_return_a_200_and_retrieve_a_task_by_id() {

        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "IA")
            .withProcessVariable("location", "17595")
            .withProcessVariable("locationName", "A Hearing Centre")
            .withProcessVariable("taskState", "assigned")
            .withProcessVariable("taskSystem", "A task system")
            .withProcessVariable("region", "A region")
            .withProcessVariable("appealType", "A appeal type")
            .withProcessVariable("securityClassification", "PUBLIC")
            .withProcessVariable("executionType", "A Execution type")
            .build();

        Map<String, String> task = setUpTaskWithCustomVariables(processVariables);

        Response result = restApiActions.get(
            "task/{task-id}",
            task.get("taskId"),
            authorizationHeadersProvider.getLawFirmAAuthorization()
        );


        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("task.id", equalTo(task.get("taskId")))
            .body("task.name", equalTo("task name"))
            .body("task.auto_assigned", equalTo(false))
            .body("task.task_state", equalTo("assigned"))
            .body("task.task_system", equalTo("A task system"))
            .body("task.location_name", equalTo("A Hearing Centre"))
            .body("task.location", equalTo("17595"))
            .body("task.security_classification", equalTo("PUBLIC"))
            .body("task.execution_type", equalTo("A Execution type"))
            .body("task.jurisdiction", equalTo("IA"))
            .body("task.region", equalTo("A region"))
            .body("task.case_category", equalTo("A appeal type"));
    }


    private Map<String, String> setUpTaskWithCustomVariables(CamundaProcessVariables processVariables) {
        Map<String, String> task = common.setupTaskAndRetrieveIds();
        given.iAddVariablesToTaskWithId(task.get("taskId"), processVariables);
        return task;
    }
}

