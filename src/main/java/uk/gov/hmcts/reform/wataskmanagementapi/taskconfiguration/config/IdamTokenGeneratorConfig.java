package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.entities.UserIdamTokenGeneratorInfo;

@Configuration
public class IdamTokenGeneratorConfig {

    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    @Bean
    public UserIdamTokenGeneratorInfo systemUserIdamInfo(
        @Value("${idam.system.username}") String systemUserName,
        @Value("${idam.system.password}") String systemUserPass,
        @Value("${idam.redirectUrl}") String idamRedirectUrl,
        @Value("${idam.scope}") String scope,
        @Value("${spring.security.oauth2.client.registration.oidc.client-id}") String clientId,
        @Value("${spring.security.oauth2.client.registration.oidc.client-secret}") String clientSecret
    ) {
        return UserIdamTokenGeneratorInfo.builder()
            .userName(systemUserName)
            .userPassword(systemUserPass)
            .idamRedirectUrl(idamRedirectUrl)
            .idamScope(scope)
            .idamClientId(clientId)
            .idamClientSecret(clientSecret)
            .build();
    }

    @Bean
    public IdamTokenGenerator systemUserIdamToken(
        UserIdamTokenGeneratorInfo systemUserIdamInfo,
        @Autowired IdamWebApi idamWebApi
    ) {
        return new IdamTokenGenerator(
            systemUserIdamInfo,
            idamWebApi
        );
    }

}
