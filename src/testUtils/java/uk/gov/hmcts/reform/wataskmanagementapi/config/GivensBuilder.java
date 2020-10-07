package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.eclipse.jetty.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;

import java.util.List;

import static java.time.ZonedDateTime.now;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper.asCamundaJsonString;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

public class GivensBuilder {

    private final String camundaUrl;

    public GivensBuilder(String camundaUrl) {
        this.camundaUrl = camundaUrl;
    }

    public GivensBuilder iCreateATaskWithCcdId(String ccdId) {

        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("ccdId", ccdId)
            .withProcessVariable("taskId", "wa-task-configuration-api-task")
            .withProcessVariable("group", "TCW")
            .withProcessVariable("dueDate", now().plusDays(2).format(CAMUNDA_DATA_TIME_FORMATTER))
            .withProcessVariable("name", "task name")
            .build();
        CamundaSendMessageRequest request = new CamundaSendMessageRequest(
            CREATE_TASK_MESSAGE.toString(),
            processVariables.getProcessVariablesMap()
        );

        given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/message")
            .body(asCamundaJsonString(request))
            .and().log().all(true)
            .when()
            .post()
            .then()
            .log().all(true)
            .and()
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT_204);
        return this;
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value) {

        String filter = "?processVariables=" + key + "_eq_" + value;

        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .basePath("/task" + filter)
            .when()
            .get()
            .then()
            .log().all(true)
            .and()
            .assertThat()
            .statusCode(HttpStatus.OK_200)
            .and()
            .extract()
            .jsonPath().getList("", CamundaTask.class);
    }

    public GivensBuilder and() {
        return this;
    }
}
