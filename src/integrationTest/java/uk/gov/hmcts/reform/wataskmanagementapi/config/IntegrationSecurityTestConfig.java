package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.HashMap;

@TestConfiguration
@Order(Ordered.LOWEST_PRECEDENCE)
@Profile({"integration","replica"})
public class IntegrationSecurityTestConfig {

    @Bean
    static BeanDefinitionRegistryPostProcessor securityOverridePostProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                removeIfFromSecurityConfiguration(registry, "securityFilterChain");
                removeIfFromSecurityConfiguration(registry, "jwtDecoder");
            }

            @Override
            public void postProcessBeanFactory(
                org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory
            ) {
                // No-op
            }

            private void removeIfFromSecurityConfiguration(BeanDefinitionRegistry registry, String beanName) {
                if (!registry.containsBeanDefinition(beanName)) {
                    return;
                }
                BeanDefinition definition = registry.getBeanDefinition(beanName);
                if ("securityConfiguration".equals(definition.getFactoryBeanName())) {
                    registry.removeBeanDefinition(beanName);
                }
            }
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "integration-test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(claims -> claims.putAll(new HashMap<>()))
            .build();
    }

    @Bean
    @Primary
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
