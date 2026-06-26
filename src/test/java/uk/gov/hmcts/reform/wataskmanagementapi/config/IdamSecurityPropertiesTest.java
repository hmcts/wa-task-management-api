package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class IdamSecurityPropertiesTest {

    private static final String IDAM_SECURITY_ALLOWED_ISSUER_0 = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String IDAM_SECURITY_ALLOWED_ISSUER_1 = "https://idam-access.aat.platform.hmcts.net/o";
    private static final String IDAM_SECURITY_ALLOWED_ISSUER_2 = "https://forgerock-am.service.core-compute-idam-perftest.internal:8443/openam/oauth2/realms/root/realms/hmcts";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindAllowedIssuers() {
        contextRunner
            .withPropertyValues(
                "idam.security.allowed-issuers[0]=" + IDAM_SECURITY_ALLOWED_ISSUER_0,
                "idam.security.allowed-issuers[1]=" + IDAM_SECURITY_ALLOWED_ISSUER_1,
                "idam.security.allowed-issuers[2]=" + IDAM_SECURITY_ALLOWED_ISSUER_2
            )
            .run(context -> assertThat(context.getBean(IdamSecurityProperties.class).getAllowedIssuers())
                .containsExactly(IDAM_SECURITY_ALLOWED_ISSUER_0, IDAM_SECURITY_ALLOWED_ISSUER_1, IDAM_SECURITY_ALLOWED_ISSUER_2));
    }

    @Configuration
    @EnableConfigurationProperties(IdamSecurityProperties.class)
    static class TestConfiguration {
    }
}
