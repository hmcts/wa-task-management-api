package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;

@Configuration
public class WaLawUserIdamTokenGeneratorConfig {

    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    @Bean
    public UserIdamTokenGeneratorInfo testUserIdamInfo(
        @Value("${idam.test.username}") String testUserName,
        @Value("${idam.test.password}") String testUserPass,
        @Value("${idam.redirectUrl}") String idamRedirectUrl,
        @Value("${idam.scope}") String scope,
        @Value("${spring.security.oauth2.client.registration.oidc.client-id}") String clientId,
        @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") String clientSecret
    ) {
        return UserIdamTokenGeneratorInfo.builder()
            .userName(testUserName)
            .userPassword(testUserPass)
            .idamRedirectUrl(idamRedirectUrl)
            .idamScope(scope)
            .idamClientId(clientId)
            .idamClientSecret(clientSecret)
            .build();
    }

    @Bean
    public IdamTokenGenerator waTestLawFirmIdamToken(
        UserIdamTokenGeneratorInfo testUserIdamInfo,
        @Autowired IdamWebApi idamServiceApi
    ) {
        return new IdamTokenGenerator(
            testUserIdamInfo,
            idamServiceApi
        );
    }
}
