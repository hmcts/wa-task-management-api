package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Assertions {

    private final RestApiActions camundaApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public Assertions(RestApiActions camundaApiActions, AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaApiActions = camundaApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
    }

    public void taskVariableWasUpdated(String taskId, String variable, String value) {

        Response result = camundaApiActions.get(
            "/history/variable-instance?taskIdIn=" + taskId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        List<HistoryVariableInstance> historyVariableInstances = result.then().assertThat()
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
