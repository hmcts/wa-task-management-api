package uk.gov.hmcts.reform.wataskmanagementapi.idam;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.Maps;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractBaseTest;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class IdamConsumerTestForPostUserInfo extends SpringBootContractBaseTest {

    private static final String IDAM_OPENID_USERINFO_URL = "/o/userinfo";

    @Pact(provider = "Idam_api", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetUserInfoDetailsAndGet200(PactDslWithProvider builder) {

        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.put("client_id", PACT_TEST_CLIENT_ID_VALUE);
        params.put("client_secret", PACT_TEST_CLIENT_SECRET_VALUE);
        params.put("scope", PACT_TEST_SCOPES_VALUE);
        params.put("username", PACT_TEST_EMAIL_VALUE);
        params.put("password", PACT_TEST_PASSWORD_VALUE);

        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        return builder
            .given("I have obtained an access_token as a user", params)
            .uponReceiving("Provider returns user info to a WA API")
            .path(IDAM_OPENID_USERINFO_URL)
            .headers(HttpHeaders.AUTHORIZATION, AUTHORIZATION_BEARER_TOKEN)
            .method(HttpMethod.GET.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseheaders)
            .body(createUserInfoResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetUserInfoDetailsAndGet200")
    public void should_get_user_info_details_with_access_token(MockServer mockServer) throws JSONException {

        Map<String, String> headers = Maps.newHashMap();
        headers.put(HttpHeaders.AUTHORIZATION, AUTHORIZATION_BEARER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String detailsResponseBody =
            SerenityRest
                .given()
                .headers(headers)
                .when()
                .get(mockServer.getUrl() + IDAM_OPENID_USERINFO_URL)
                .then()
                .statusCode(200)
                .and()
                .extract()
                .body()
                .asString();

        JSONObject response = new JSONObject(detailsResponseBody);

        assertThat(detailsResponseBody).isNotNull();
        assertThat(response).hasNoNullFieldsOrProperties();
        assertThat(response.getString("uid")).isNotBlank();
        assertThat(response.getString("givenName")).isNotBlank();
        assertThat(response.getString("familyName")).isNotBlank();
        JSONArray rolesArr = response.getJSONArray("roles");
        assertThat(rolesArr).isNotNull();
        assertThat(rolesArr.length()).isNotZero();
        assertThat(rolesArr.get(0).toString()).isNotBlank();

    }

    private PactDslJsonBody createUserInfoResponse() {

        return new PactDslJsonBody()
            .stringType("uid", "1111-2222-3333-4567")
            .stringValue("sub", PACT_TEST_EMAIL_VALUE)
            .stringValue("givenName", "Case")
            .stringValue("familyName", "Officer")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("caseworker-ia-legalrep-solicitor"), 1);
    }
}
