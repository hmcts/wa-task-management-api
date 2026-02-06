package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.Map;

@TestConfiguration
@Profile("integration")
public class IntegrationIdamStubConfig {

    @Bean
    @Primary
    public IdamWebApi idamWebApi() {
        return new IdamWebApi() {
            @Override
            public UserInfo userInfo(String userToken) {
                return UserInfo.builder()
                    .uid(ServiceMocks.IDAM_USER_ID)
                    .email(ServiceMocks.IDAM_USER_EMAIL)
                    .build();
            }

            @Override
            public Token token(Map<String, ?> form) {
                return new Token(ServiceMocks.IDAM_AUTHORIZATION_TOKEN, "scope");
            }
        };
    }

    @Bean
    @Primary
    public IdamServiceApi idamServiceApi() {
        return new IdamServiceApi() {
            @Override
            public void createTestUser(Map<String, ?> form) {
                // no-op for integration tests
            }

            @Override
            public void deleteTestUser(String username) {
                // no-op for integration tests
            }
        };
    }

}
