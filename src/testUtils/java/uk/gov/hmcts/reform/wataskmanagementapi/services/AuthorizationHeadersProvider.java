package uk.gov.hmcts.reform.wataskmanagementapi.services;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.RoleCode;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAccount;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Slf4j
@Service
public class AuthorizationHeadersProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    private final Map<String, TestAccount> accounts = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}") protected String idamRedirectUrl;
    @Value("${idam.scope}") protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}") protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") protected String idamClientSecret;
    @Autowired
    private IdamWebApi idamWebApi;
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

    public Headers getTribunalCaseworkerAAuthorization(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        return new Headers(
            getCaseworkerAAuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getTribunalCaseworkerBAuthorization(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        return new Headers(
            getCaseworkerBAuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getTribunalCaseworkerR2Authorization(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        return new Headers(
            getCaseworkerR2AuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );
    }

    public Headers getLawFirmAuthorization() {
        /*
         * This user is used to create cases in ccd
         */
        return new Headers(
            getLawFirmAuthorizationOnly(),
            getServiceAuthorizationHeader()
        );
    }


    public Header getCaseworkerAAuthorizationOnly(String emailPrefix) {

        String key = "Caseworker A";

        TestAccount caseworker = getIdamCredentials(key, emailPrefix);
        return getAuthorization(key, caseworker.getUsername(), caseworker.getPassword());

    }

    public Header getCaseworkerBAuthorizationOnly(String emailPrefix) {

        String key = "Caseworker B";

        TestAccount caseworker = getIdamCredentials(key, emailPrefix);
        return getAuthorization(key, caseworker.getUsername(), caseworker.getPassword());

    }

    public Header getCaseworkerR2AuthorizationOnly(String emailPrefix) {

        String key = "Caseworker R2";

        TestAccount caseworker = getIdamCredentials(key, emailPrefix);
        return getAuthorization(key, caseworker.getUsername(), caseworker.getPassword());

    }

    public Header getLawFirmAuthorizationOnly() {

        String username = System.getenv("TEST_WA_LAW_FIRM_USERNAME");
        String password = System.getenv("TEST_WA_LAW_FIRM_PASSWORD");

        return getAuthorization("LawFirm", username, password);

    }

    private Header getAuthorization(String key, String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            key,
            user -> "Bearer " + idamWebApi.token(body).getAccessToken()
        );
        return new Header(AUTHORIZATION, accessToken);
    }

    private TestAccount getIdamCredentials(String key, String emailPrefix) {

        return accounts.computeIfAbsent(
            key,
            user -> generateIdamTestAccount(emailPrefix)
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

    private TestAccount generateIdamTestAccount(String emailPrefix) {
        String email = emailPrefix + UUID.randomUUID() + "@fake.hmcts.net";
        String password = "London01";

        log.info("Attempting to create a new test account {}", email);

        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-ia"), new RoleCode("caseworker-ia-caseofficer"));
        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("forename", "WAFTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);

        idamServiceApi.createTestUser(body);

        log.info("Test account created successfully");
        return new TestAccount(email, password);
    }

    public UserInfo getUserInfo(String userToken) {
        return userInfo.computeIfAbsent(
            userToken,
            user -> idamWebApi.userInfo(userToken)
        );

    }

    public Headers getServiceAuthorizationHeadersOnly() {
        return new Headers(getServiceAuthorizationHeader());
    }
}
