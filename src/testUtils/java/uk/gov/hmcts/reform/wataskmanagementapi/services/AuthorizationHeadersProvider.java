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
    private IdamServiceApi idamServiceApi;

    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;

    public Header getServiceAuthorizationHeader() {
        String serviceToken = tokens.computeIfAbsent(
            SERVICE_AUTHORIZATION,
            user -> serviceAuthTokenGenerator.generate()
        );

        return new Header(SERVICE_AUTHORIZATION, serviceToken);
    }

    public Headers getTribunalCaseworkerAAuthorization() {
        return new Headers(
            getCaseworkerAAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }


    public Headers getLawFirmAAuthorization() {
        return new Headers(
            getLawFirmAAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }


    public Headers getLawFirmBAuthorization() {
        return new Headers(
            getLawFirmBAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }

    public Header getCaseworkerAAuthorizationOnly() {

        String username = System.getenv("TEST_WA_CASEOFFICER_A_USERNAME");
        String password = System.getenv("TEST_WA_CASEOFFICER_A_PASSWORD");

        return getAuthorization("CaseworkerA", username, password);

    }


    public Header getLawFirmAAuthorizationOnly() {

        String username = System.getenv("TEST_LAW_FIRM_A_USERNAME");
        String password = System.getenv("TEST_LAW_FIRM_A_PASSWORD");

        return getAuthorization("LawFirmA", username, password);

    }

    public Header getLawFirmBAuthorizationOnly() {

        String username = System.getenv("TEST_LAW_FIRM_B_USERNAME");
        String password = System.getenv("TEST_LAW_FIRM_B_PASSWORD");

        return getAuthorization("LawFirmB", username, password);
    }


    private Header getAuthorization(String key, String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            key,
            user -> "Bearer " + idamServiceApi.token(body).getAccessToken()
        );

        return new Header(AUTHORIZATION, accessToken);
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
