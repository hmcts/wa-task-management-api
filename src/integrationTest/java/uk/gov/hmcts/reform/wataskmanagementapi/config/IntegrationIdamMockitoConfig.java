package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.mockito.Mockito;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.services.IdamServiceApi;

@TestConfiguration
@Profile({"integration", "replica"})
public class IntegrationIdamMockitoConfig {

    private static final String IDAM_WEB_API_BEAN =
        "uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi";
    private static final String IDAM_SERVICE_API_BEAN =
        "uk.gov.hmcts.reform.wataskmanagementapi.services.IdamServiceApi";

    @Bean
    static BeanDefinitionRegistryPostProcessor idamFeignOverridePostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition(IDAM_WEB_API_BEAN)) {
                    registry.getBeanDefinition(IDAM_WEB_API_BEAN).setPrimary(false);
                }
                if (registry.containsBeanDefinition(IDAM_SERVICE_API_BEAN)) {
                    registry.getBeanDefinition(IDAM_SERVICE_API_BEAN).setPrimary(false);
                }
            }

            @Override
            public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory
            ) {
                // No-op
            }
        };
    }

    @Bean
    @Primary
    public IdamWebApi idamWebApi() {
        return Mockito.mock(IdamWebApi.class);
    }

    @Bean
    @Primary
    public IdamServiceApi idamServiceApi() {
        return Mockito.mock(IdamServiceApi.class);
    }

    @Bean
    public IdamMockitoStubber idamMockitoStubber(IdamWebApi idamWebApi, IdamServiceApi idamServiceApi) {
        return new IdamMockitoStubber(idamWebApi, idamServiceApi);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("oidc")
            .clientId("integration-test")
            .clientSecret("integration-test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile")
            .authorizationUri("http://localhost/oauth2/authorize")
            .tokenUri("http://localhost/oauth2/token")
            .userInfoUri("http://localhost/userinfo")
            .userNameAttributeName("sub")
            .jwkSetUri("http://localhost/jwks")
            .clientName("oidc")
            .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    public OAuth2AuthorizedClientService oauth2AuthorizedClientService(
        ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository oauth2AuthorizedClientRepository(
        OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }
}
