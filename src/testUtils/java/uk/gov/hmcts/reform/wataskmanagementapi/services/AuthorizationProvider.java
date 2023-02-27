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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.RoleCode;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAccount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
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
            log.info("Deleting test account '{}'", username);
            idamServiceApi.deleteTestUser(username);
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

    public TestAuthenticationCredentials getNewLawFirm() {
        /*
         * This user is used to create cases in ccd
         */
        TestAccount lawfirm = getIdamLawFirmCredentials("wa-ft-lawfirm-");

        Headers authenticationHeaders = new Headers(
            getAuthorizationOnly(lawfirm),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(lawfirm, authenticationHeaders);
    }

    public TestAuthenticationCredentials getJudgeAuthorization(String emailPrefix) {
        /*
         * This user is used to create cases in ccd
         */
        TestAccount lawfirm = getIdamJudgeCredentials(emailPrefix);

        Headers headers = new Headers(
            getJudgeAuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );

        return new TestAuthenticationCredentials(lawfirm, headers);
    }

    public TestAuthenticationCredentials getAdminCaseworker(String emailPrefix) {
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

    public TestAuthenticationCredentials getAdminUserAuthorization(String emailPrefix) {
        /*
         * This user is used to create cases in ccd
         */
        TestAccount caseworker = getAdministativeCredentials(emailPrefix);

        Headers authenticationHeaders = new Headers(
            getAdminUserAuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );
        return new TestAuthenticationCredentials(caseworker, authenticationHeaders);

    }

    public Header getCaseworkerAuthorizationOnly(String emailPrefix) {
        TestAccount caseworker = getIdamCaseWorkerCredentials(emailPrefix);
        return getAuthorization(caseworker.getUsername(), caseworker.getPassword());

    }

    public Header getLawFirmAuthorizationOnly() {

        TestAccount lawfirm = getIdamLawFirmCredentials("wa-ft-lawfirm-");
        return getAuthorization(lawfirm.getUsername(), lawfirm.getPassword());

    }

    public Header getAuthorizationOnly(TestAccount account) {
        return getAuthorization(account.getUsername(), account.getPassword());
    }

    public Header getJudgeAuthorizationOnly(String emailPrefix) {

        TestAccount lawfirm = getIdamJudgeCredentials(emailPrefix);
        return getAuthorization(lawfirm.getUsername(), lawfirm.getPassword());

    }

    public Header getAdminUserAuthorizationOnly(String emailPrefix) {

        TestAccount lawfirm = getAdministativeCredentials(emailPrefix);
        return getAuthorization(lawfirm.getUsername(), lawfirm.getPassword());

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

    public Headers getWACaseworkerAAuthorization(String emailPrefix) {
        /*
         * This user is used to assign role assignments to on a per test basis.
         * A clean up before assigning new role assignments is needed.
         */
        return new Headers(
            getWACaseworkerAAuthorizationOnly(emailPrefix),
            getServiceAuthorizationHeader()
        );
    }

    public Header getWACaseworkerAAuthorizationOnly(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-wa-task-configuration"),
            new RoleCode("payments"),
            new RoleCode("caseworker-wa"));
        TestAccount testAccount = generateIdamTestAccount(emailPrefix, requiredRoles);
        return getAuthorization(testAccount.getUsername(), testAccount.getPassword());

    }

    public String getUserId(Headers headers) {
        return getUserInfo(headers.getValue(AUTHORIZATION)).getUid();
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

    private TestAccount getIdamJudgeCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-ia"),
            new RoleCode("caseworker-ia-judiciary"),
            new RoleCode("payments")
        );
        return generateIdamTestAccount(emailPrefix, requiredRoles);
    }

    private TestAccount getAdministativeCredentials(String emailPrefix) {
        List<RoleCode> requiredRoles = asList(new RoleCode("caseworker-ia"),
            new RoleCode("caseworker-ia-admofficer"),
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
        String email = emailPrefix + UUID.randomUUID() + "@fake.hmcts.net";
        String password = "London01";

        log.info("Attempting to create a new test account {}", email);

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
}
