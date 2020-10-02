package uk.gov.hmcts.reform.wataskmanagementapi.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Service
public class AuthorizationHeadersProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}") protected String idamRedirectUrl;
    @Value("${idam.scope}") protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}") protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") protected String idamClientSecret;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    @Autowired
    private IdamServiceApi idamServiceApi;


    public Headers getServiceAuthorizationOnly() {
        String serviceToken = getServiceToken();
        return new Headers(
            new Header(SERVICE_AUTHORIZATION, serviceToken)
        );
    }

    public Headers getCaseOfficerAuthorization() {

        String username = System.getenv("TEST_LAW_FIRM_A_USERNAME");
        String password = System.getenv("TEST_LAW_FIRM_A_PASSWORD");

        MultiValueMap<String, String> body = getAuthTokenRequestBody(username, password);

        String accessToken = getAccessToken("CaseOfficer", body);
        String serviceToken = getServiceToken();

        return new Headers(
            new Header(AUTHORIZATION, accessToken),
            new Header(SERVICE_AUTHORIZATION, serviceToken)
        );
    }

    private String getServiceToken() {
        return tokens.computeIfAbsent(
            "ServiceAuth",
            user -> serviceAuthTokenGenerator.generate()
        );
    }

    private String getAccessToken(String key, MultiValueMap<String, String> body) {
        return tokens.computeIfAbsent(
            key,
            user -> "Bearer " + idamServiceApi.token(body).getAccessToken()
        );
    }

    private MultiValueMap<String, String> getAuthTokenRequestBody(String username, String password) {
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
