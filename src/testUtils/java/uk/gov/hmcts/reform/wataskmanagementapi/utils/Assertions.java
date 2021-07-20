package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Assertions {

    private final RestApiActions camundaApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public Assertions(RestApiActions camundaApiActions, AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaApiActions = camundaApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
    }

    public void taskVariableWasUpdated(String processInstanceId, String variable, String value) {

        Response result = camundaApiActions.get(
            "/history/variable-instance?processInstanceId=" + processInstanceId,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        List<HistoryVariableInstance> historyVariableInstances = result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract()
            .jsonPath().getList("", HistoryVariableInstance.class);

        List<HistoryVariableInstance> taskStateHistory = historyVariableInstances.stream()
            .filter(historyVariableInstance -> historyVariableInstance.getName().equals(variable))
            .collect(Collectors.toList());

        //Entire history of the variable including multiple scopes we assert that it contains the expected entry
        assertNotNull(taskStateHistory.get(0).getId());
        assertEquals(taskStateHistory.get(0).getName(), variable);
        assertEquals(taskStateHistory.get(0).getValue(), value);
    }
}
