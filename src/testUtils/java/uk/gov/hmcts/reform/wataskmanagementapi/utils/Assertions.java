package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static net.serenitybdd.rest.SerenityRest.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class Assertions {

    private final String camundaUrl;

    public Assertions(String camundaUrl) {
        this.camundaUrl = camundaUrl;
    }

    public void taskVariableWasUpdated(String taskId, String variable, String value) {
        List<HistoryVariableInstance> historyVariableInstances = given()
            .contentType(APPLICATION_JSON_VALUE)
            .baseUri(camundaUrl)
            .when()
            .get("/history/variable-instance?taskIdIn=" + taskId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract()
            .jsonPath().getList("", HistoryVariableInstance.class);

        List<HistoryVariableInstance> taskState = historyVariableInstances.stream()
            .filter(historyVariableInstance -> historyVariableInstance.getName().equals(variable))
            .collect(Collectors.toList());

        assertThat(taskState, is(singletonList(new HistoryVariableInstance(variable, value))));
    }
}
