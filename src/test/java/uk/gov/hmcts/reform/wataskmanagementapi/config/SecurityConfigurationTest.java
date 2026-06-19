package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private static final String IDAM_WEB_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String FORGEROCK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";
    private static final String UNTRUSTED_ISSUER = "https://untrusted.example.com/o";

    @Test
    void shouldValidateJwtWithAnyAllowedIssuer() {
        List<String> allowedIssuers = List.of(IDAM_WEB_ISSUER, FORGEROCK_ISSUER);

        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(allowedIssuers)
            .validate(jwtWithIssuer(FORGEROCK_ISSUER, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldRejectJwtWithUntrustedIssuer() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(IDAM_WEB_ISSUER, FORGEROCK_ISSUER))
            .validate(jwtWithIssuer(UNTRUSTED_ISSUER, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldKeepDefaultJwtValidation() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(IDAM_WEB_ISSUER))
            .validate(jwtWithIssuer(IDAM_WEB_ISSUER, Instant.now().minusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwtWithIssuer(String issuer, Instant expiresAt) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("iss", issuer)
            .subject("user")
            .issuedAt(Instant.now())
            .expiresAt(expiresAt)
            .build();
    }
}
