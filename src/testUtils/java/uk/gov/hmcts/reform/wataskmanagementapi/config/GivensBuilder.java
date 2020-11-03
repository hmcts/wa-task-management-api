package uk.gov.hmcts.reform.wataskmanagementapi.config;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSendMessageRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

import java.util.List;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaMessage.CREATE_TASK_MESSAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaProcessVariables.ProcessVariablesBuilder.processVariables;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

public class GivensBuilder {

    private final String camundaUrl;
    private final CamundaObjectMapper camundaObjectMapper;

    public GivensBuilder(String camundaUrl, CamundaObjectMapper camundaObjectMapper) {
        this.camundaUrl = camundaUrl;
        this.camundaObjectMapper = camundaObjectMapper;
    }

    public GivensBuilder iCreateATaskWithCcdId(String ccdId) {

        CamundaProcessVariables processVariables = processVariables()
            .withProcessVariable("jurisdiction", "IA")
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
            .body(camundaObjectMapper.asCamundaJsonString(request))
            .when()
            .post("/message")
            .then()
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        return this;
    }

    public List<CamundaTask> iRetrieveATaskWithProcessVariableFilter(String key, String value) {

        String filter = "?processVariables=" + key + "_eq_" + value;

        return given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .when()
            .get("/task" + filter)
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract()
            .jsonPath().getList("", CamundaTask.class);
    }

    public GivensBuilder and() {
        return this;
    }


    public void iClaimATaskWithIdAndAuthorization(String taskId, Headers headers) {
        Response response = given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .headers(headers)
            .when()
            .post("task/{task-id}/claim", taskId);

        response.then().assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
    }

    public GivensBuilder iAddVariablesToTaskWithId(String taskId, CamundaProcessVariables processVariables) {
        given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .body(new Modifications(processVariables.getProcessVariablesMap()))
            .when()
            .post("/task/{task-id}/variables", taskId)
            .then()
            .log().all(true)
            .assertThat()
            .statusCode(HttpStatus.NO_CONTENT.value());
        return this;
    }

    private class Modifications {
        private final Map<String, CamundaValue<?>> modifications;

        public Modifications(Map<String, CamundaValue<?>> processVariablesMap) {
            super();
            this.modifications = processVariablesMap;
        }

        public Map<String, CamundaValue<?>> getModifications() {
            return modifications;
        }
    }
}
