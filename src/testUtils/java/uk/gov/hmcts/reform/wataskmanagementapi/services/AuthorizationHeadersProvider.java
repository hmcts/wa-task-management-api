package uk.gov.hmcts.reform.wataskmanagementapi.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthorizationHeadersProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}") protected String idamRedirectUrl;
    @Value("${idam.scope}") protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}") protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") protected String idamClientSecret;

    @Autowired
    private IdamServiceApi idamServiceApi;

    public Headers getLawFirmAAuthorization() {

        String username = System.getenv("TEST_LAW_FIRM_A_USERNAME");
        String password = System.getenv("TEST_LAW_FIRM_A_PASSWORD");


        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            "LawFirmA",
            user -> "Bearer " + idamServiceApi.token(body).getAccessToken()
        );

        return new Headers(
            new Header("Authorization", accessToken)
        );
    }


    public Headers getLawFirmBAuthorization() {

        String username = System.getenv("TEST_LAW_FIRM_B_USERNAME");
        String password = System.getenv("TEST_LAW_FIRM_B_PASSWORD");

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            "LawFirmB",
            user -> "Bearer " + idamServiceApi.token(body).getAccessToken()
        );

        return new Headers(
            new Header("Authorization", accessToken)
        );
    }


    private MultiValueMap<String, String> createIdamRequest(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("redirect_uri", idamRedirectUrl);
        body.add("client_id", idamClientId);
        body.add("client_secret", idamClientSecret);
        body.add("username", username);
        body.add("password", password);
        body.add("scope", userScope);

        return body;
    }
}
