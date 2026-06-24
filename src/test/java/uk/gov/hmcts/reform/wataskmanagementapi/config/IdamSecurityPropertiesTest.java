package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class IdamSecurityPropertiesTest {

    private static final String IDAM_WEB_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String IDAM_ACCESS_ISSUER = "https://idam-access.aat.platform.hmcts.net/o";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindAllowedIssuers() {
        contextRunner
            .withPropertyValues(
                "idam.security.allowed-issuers[0]=" + IDAM_WEB_ISSUER,
                "idam.security.allowed-issuers[1]=" + IDAM_ACCESS_ISSUER
            )
            .run(context -> assertThat(context.getBean(IdamSecurityProperties.class).getAllowedIssuers())
                .containsExactly(IDAM_WEB_ISSUER, IDAM_ACCESS_ISSUER));
    }

    @Configuration
    @EnableConfigurationProperties(IdamSecurityProperties.class)
    static class TestConfiguration {
    }
}
