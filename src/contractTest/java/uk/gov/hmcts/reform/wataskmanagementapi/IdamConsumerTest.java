package uk.gov.hmcts.reform.wataskmanagementapi;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.model.RequestResponsePact;
import com.google.common.collect.Maps;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.rest.SerenityRest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(PactConsumerTestExt.class)
@ExtendWith(SpringExtension.class)
public class IdamConsumerTest {

    private static final String BEARER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdeRre";
    private static final String IDAM_OPENID_TOKEN_URL = "/o/token";
    private static final String IDAM_OPENID_USERINFO_URL = "/o/userinfo";

    private static final String PACT_TEST_EMAIL_VALUE = "ia-caseofficer@fake.hmcts.net";
    private static final String PACT_TEST_PASSWORD_VALUE = "London01";
    private static final String PACT_TEST_CLIENT_ID_VALUE = "pact";
    private static final String PACT_TEST_CLIENT_SECRET_VALUE = "pactsecret";
    private static final String PACT_TEST_SCOPES_VALUE = "openid profile roles";
    private static final String PACT_TEST_ROLES_VALUE = "caseworker";

    @Pact(provider = "Idam_api", consumer = "wa_task_management_api")
    public RequestResponsePact executeGetIdamAccessTokenAndGet200(PactDslWithProvider builder) throws JSONException {

        Map<String, String> responseheaders = Maps.newHashMap();
        responseheaders.put("Content-Type", "application/json");

        Map<String, Object> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        params.put("email", PACT_TEST_EMAIL_VALUE);
        params.put("password", PACT_TEST_PASSWORD_VALUE);
        params.put("forename", "Case");
        params.put("surname", "Officer");
        params.put("roles", singletonList(PACT_TEST_ROLES_VALUE));

        return builder
            .given("a user exists", params)
            .uponReceiving("Provider receives a POST /o/token request from a WA API")
            .path(IDAM_OPENID_TOKEN_URL)
            .method(HttpMethod.POST.toString())
            .body(
                "client_id=" + PACT_TEST_CLIENT_ID_VALUE
                + "&client_secret=" + PACT_TEST_CLIENT_SECRET_VALUE
                + "&grant_type=password"
                + "&scope=" + PACT_TEST_SCOPES_VALUE
                + "&username=" + PACT_TEST_EMAIL_VALUE
                + "&password=" + PACT_TEST_PASSWORD_VALUE
                + "&redirect_uri=http%3A%2F%2Fwww.dummy-pact-service.com%2Fcallback",
                ContentType.URLENC.toString())
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .headers(responseheaders)
            .body(createAuthResponse())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "executeGetIdamAccessTokenAndGet200")
    public void should_post_to_token_endpoint_and_receive_access_token_with_200_response(MockServer mockServer)
        throws JSONException {
        String actualResponseBody =
            SerenityRest
                .given()
                .contentType(ContentType.URLENC)
                .formParam("redirect_uri", "http://www.dummy-pact-service.com/callback")
                .formParam("client_id", PACT_TEST_CLIENT_ID_VALUE)
                .formParam("client_secret", PACT_TEST_CLIENT_SECRET_VALUE)
                .formParam("grant_type", "password")
                .formParam("username", PACT_TEST_EMAIL_VALUE)
                .formParam("password", PACT_TEST_PASSWORD_VALUE)
                .formParam("scope", PACT_TEST_SCOPES_VALUE)
                .post(mockServer.getUrl() + IDAM_OPENID_TOKEN_URL)
                .then()
                .extract().asString();

        JSONObject response = new JSONObject(actualResponseBody);

        assertThat(response).isNotNull();
        assertThat(response.getString("access_token")).isNotBlank();
        assertThat(response.getString("token_type")).isEqualTo("Bearer");
        assertThat(response.getString("expires_in")).isNotBlank();

    }

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
            .headers(HttpHeaders.AUTHORIZATION, BEARER_TOKEN)
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
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_TOKEN);
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

    private PactDslJsonBody createAuthResponse() {

        return new PactDslJsonBody()
            .stringType("access_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("refresh_token", "eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiYi9PNk92V")
            .stringType("scope", PACT_TEST_SCOPES_VALUE)
            .stringType("id_token", "eyJ0eXAiOiJKV1QiLCJraWQiOiJiL082T3ZWdjEre")
            .stringType("token_type", "Bearer")
            .stringType("expires_in", "28798");
    }

    private PactDslJsonBody createUserInfoResponse() {

        return new PactDslJsonBody()
            .stringType("uid", "1111-2222-3333-4567")
            .stringValue("sub", PACT_TEST_EMAIL_VALUE)
            .stringValue("givenName", "Case")
            .stringValue("familyName", "Officer")
            .minArrayLike("roles", 1, PactDslJsonRootValue.stringType("caseworker-ia-legalrep-solicitor"), 1)
            .stringType("IDAM_ADMIN_USER");
    }
}
