package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.RoleCode;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAccount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;

@Slf4j
@Service
public class AuthorizationProvider {

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> userInfo = new ConcurrentHashMap<>();
    @Value("${idam.redirectUrl}") protected String idamRedirectUrl;
    @Value("${idam.scope}") protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}") protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") protected String idamClientSecret;
    @Value("${idam.test.test-account-pw:default}") protected String idamTestAccountPassword;
    @Autowired
    private IdamWebApi idamWebApi;
    @Autowired
    private IdamServiceApi idamServiceApi;
    @Autowired
    private AuthTokenGenerator serviceAuthTokenGenerator;
    @Value("${idam.test.userCleanupEnabled:false}")
    private boolean testUserDeletionEnabled;

    public void deleteAccount(String username) {

        if (testUserDeletionEnabled) {
            //If error is thrown while deleting the user, it will be caught and logged
            try {
                log.info("Deleting test account '{}'", username);
                idamServiceApi.deleteTestUser(username);
            } catch (FeignException e) {
                log.error("Failed to delete test account '{}'", username, e);
            }
        } else {
            log.info("Test User deletion feature flag was not enabled, user '{}' was not deleted", username);
        }
    }

    public Header getServiceAuthorizationHeader() {
        return new Header(SERVICE_AUTHORIZATION, serviceAuthTokenGenerator.generate());
    }

    public TestAuthenticationCredentials getNewTribunalCaseworker(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        TestAccount caseworker = getIdamLawFirmCredentials(emailPrefix);

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(caseworker),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(caseworker, authenticationHeaders);
    }

    public TestAuthenticationCredentials getNewWaTribunalCaseworker(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        TestAccount caseworker = getIdamWaTribunalCaseworkerCredentials(emailPrefix);

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(caseworker),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(caseworker, authenticationHeaders);
    }

    public TestAuthenticationCredentials getNewWaTribunalCaseworkerWithStaticEmail(String email) {

        TestAccount caseworker = getIdamWaTribunalCaseworkerCredentialsWithStaticEmail(email);

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(caseworker),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(caseworker, authenticationHeaders);
    }

    public Header getCaseworkerAuthorizationOnly(String emailPrefix) {
        TestAccount caseworker = getIdamCaseWorkerCredentials(emailPrefix);
        return getAuthorization(caseworker.getUsername(), caseworker.getPassword());

    }

    public Header getAuthorizationOnly(TestAccount account) {
        return getAuthorization(account.getUsername(), account.getPassword());
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

    private Header getAuthorization(String username, String password) {

        MultiValueMap<String, String> body = createIdamRequest(username, password);

        String accessToken = tokens.computeIfAbsent(
            username,
            user -> "Bearer " + idamWebApi.token(body).getAccessToken()
        );

        return new Header(AUTHORIZATION, accessToken);
    }

    private TestAccount getIdamCaseWorkerCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-ia"), new RoleCode("caseworker-ia-caseofficer"));
        return generateIdamTestAccount(emailPrefix, requiredRoles);
    }

    private TestAccount getIdamLawFirmCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-ia"),
            new RoleCode("caseworker-ia-legalrep-solicitor"),
            new RoleCode("payments")
        );
        return generateIdamTestAccount(emailPrefix, requiredRoles);
    }

    private TestAccount getIdamWaTribunalCaseworkerCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-wa-task-configuration"),
            new RoleCode("payments"),
            new RoleCode("caseworker-wa"));
        return generateIdamTestAccount(emailPrefix, requiredRoles);
    }

    private TestAccount getIdamWaTribunalCaseworkerCredentialsWithStaticEmail(String email) {
        List<RoleCode> requiredRoles = asList(new RoleCode("case-manager"),
                                              new RoleCode("senior-tribunal-caseworker"));
        return generateIdamTestAccountWithStaticEmail(email, requiredRoles);
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

    private TestAccount generateIdamTestAccount(String emailPrefix, List<RoleCode> requiredRoles) {
        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        body.put("password", idamTestAccountPassword);
        body.put("forename", "WAFTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);
        AtomicBoolean accountCreated = new AtomicBoolean(false);
        AtomicReference<String> email = new AtomicReference<>("");
        await().ignoreException(Exception.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(120, SECONDS)
            .until(() -> {
                try {
                    email.set(emailPrefix + UUID.randomUUID() + "@fake.hmcts.net");
                    log.info("Attempting to create a new test account {}", email);
                    body.put("email", email);
                    idamServiceApi.createTestUser(body);
                    accountCreated.set(true);
                } catch (FeignException e) {
                    log.error("Failed to create test account, retrying...", e);
                    accountCreated.set(false);
                }
                return accountCreated.get();
            });

        log.info("Test account created successfully");
        return new TestAccount(email.get(), idamTestAccountPassword);
    }

    private TestAccount generateIdamTestAccountWithStaticEmail(String emailId, List<RoleCode> requiredRoles) {
        RoleCode userGroup = new RoleCode("caseworker");

        Map<String, Object> body = new ConcurrentHashMap<>();
        String staticEmail = emailId + "@fake.hmcts.net";
        body.put("id",UUID.nameUUIDFromBytes(staticEmail.getBytes()).toString());
        body.put("password", idamTestAccountPassword);
        body.put("forename", "WAFTAccount");
        body.put("surname", "Functional");
        body.put("roles", requiredRoles);
        body.put("userGroup", userGroup);
        AtomicBoolean accountCreated = new AtomicBoolean(false);
        AtomicReference<String> email = new AtomicReference<>("");
        await().ignoreException(Exception.class)
            .pollInterval(500, MILLISECONDS)
            .atMost(120, SECONDS)
            .until(() -> {
                try {
                    email.set(staticEmail);
                    log.info("Attempting to create a new test account {}", email);
                    body.put("email", email);
                    idamServiceApi.createTestUser(body);
                    accountCreated.set(true);
                } catch (FeignException e) {
                    log.error("Failed to create test account, retrying...", e);
                    accountCreated.set(false);
                }
                return accountCreated.get();
            });

        log.info("Test account created successfully");
        return new TestAccount(email.get(), idamTestAccountPassword);
    }
}
