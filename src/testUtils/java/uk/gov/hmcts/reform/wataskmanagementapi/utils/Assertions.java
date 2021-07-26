package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import io.restassured.response.Response;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.util.Map;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class Assertions {

    private final RestApiActions camundaApiActions;
    private final AuthorizationHeadersProvider authorizationHeadersProvider;

    public Assertions(RestApiActions camundaApiActions, AuthorizationHeadersProvider authorizationHeadersProvider) {
        this.camundaApiActions = camundaApiActions;
        this.authorizationHeadersProvider = authorizationHeadersProvider;
    }

    public void taskVariableWasUpdated(String processInstanceId, String variable, String value) {

        Map<String, Object> request = Map.of(
            "variableName", variable,
            "processInstanceId", processInstanceId
        );

        Response result = camundaApiActions.post(
            "/history/variable-instance",
            request,
            authorizationHeadersProvider.getServiceAuthorizationHeader()
        );

        result.prettyPrint();

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("name", everyItem(is(variable)))
            .body("value", hasItem(value));
    }
}
