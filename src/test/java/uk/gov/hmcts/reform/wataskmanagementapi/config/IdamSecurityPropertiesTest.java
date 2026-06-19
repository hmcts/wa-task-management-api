package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class IdamSecurityPropertiesTest {

    private static final String IDAM_WEB_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String FORGEROCK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindAllowedIssuers() {
        contextRunner
            .withPropertyValues(
                "idam.security.allowed-issuers[0]=" + IDAM_WEB_ISSUER,
                "idam.security.allowed-issuers[1]=" + FORGEROCK_ISSUER
            )
            .run(context -> assertThat(context.getBean(IdamSecurityProperties.class).getAllowedIssuers())
                .containsExactly(IDAM_WEB_ISSUER, FORGEROCK_ISSUER));
    }

    @Configuration
    @EnableConfigurationProperties(IdamSecurityProperties.class)
    static class TestConfiguration {
    }
}
