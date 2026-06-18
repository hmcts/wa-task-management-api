package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private static final String ALLOWED_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String DIFFERENT_ISSUER = "https://hmcts-access.service.gov.uk/o";

    @Test
    void shouldValidateJwtWithAllowedIssuer() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(ALLOWED_ISSUER))
            .validate(jwtWithIssuer(ALLOWED_ISSUER, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldRejectJwtWithDifferentIssuer() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(ALLOWED_ISSUER))
            .validate(jwtWithIssuer(DIFFERENT_ISSUER, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldKeepDefaultJwtValidation() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(ALLOWED_ISSUER))
            .validate(jwtWithIssuer(ALLOWED_ISSUER, Instant.now().minusSeconds(60)));

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
