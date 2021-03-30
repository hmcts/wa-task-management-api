package uk.gov.hmcts.reform.wataskmanagementapi.consumer.idam;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {uk.gov.hmcts.reform.wataskmanagementapi.consumer.idam.IdamConsumerApplication.class})
public class IdamConsumerTestForPostToken extends SpringBootContractBaseTest {

    @Autowired
    IdamWebApi idamApi;

    @Pact(provider = "idamApi_oidc", consumer = "wa_task_management_api")
    public RequestResponsePact generatePactFragmentToken(PactDslWithProvider builder) throws JSONException {

        Map<String, String> responseheaders = ImmutableMap.<String, String>builder()
            .put("Content-Type", "application/json")
            .build();

        return builder
            .given("a token is requested")
            .uponReceiving("Provider receives a POST /o/token request from a WA API")
            .path("/o/token")
            .method(HttpMethod.POST.toString())
            .body("redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback"
                  + "&client_id=" + PACT_TEST_CLIENT_ID_VALUE
                  + "&grant_type=password"
                  + "&username=" + PACT_TEST_EMAIL_VALUE
                  + "&password=" + PACT_TEST_PASSWORD_VALUE
                  + "&client_secret=" + PACT_TEST_CLIENT_SECRET_VALUE
                  + "&scope=" + PACT_TEST_SCOPES_VALUE,
                "application/x-www-form-urlencoded")
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseheaders)
            .body(createAuthResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragmentToken")
    public void verifyIdamUserDetailsRolesPactToken() {

        Map<String, String> tokenRequestMap = buildTokenRequestMap();
        Token token = idamApi.token(tokenRequestMap);
        assertEquals("eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre", token.getAccessToken(), "Token is not expected");
    }

    private Map<String, String> buildTokenRequestMap() {
        Map<String, String> tokenRequestMap = ImmutableMap.<String, String>builder()
            .put("redirect_uri", "http://www.dummy-pact-service.com/callback")
            .put("client_id", PACT_TEST_CLIENT_ID_VALUE)
            .put("client_secret", PACT_TEST_CLIENT_SECRET_VALUE)
            .put("grant_type", "password")
            .put("username", PACT_TEST_EMAIL_VALUE)
            .put("password", PACT_TEST_PASSWORD_VALUE)
            .put("scope", PACT_TEST_ROLES_VALUE)
            .build();
        return tokenRequestMap;
    }

    private PactDslJsonBody createAuthResponse() {

        return new PactDslJsonBody()
            .stringType("access_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("scope", "openid roles profile");
    }
}
