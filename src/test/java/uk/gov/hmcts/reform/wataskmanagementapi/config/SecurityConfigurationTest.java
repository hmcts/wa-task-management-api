package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String INVALID_ISSUER = "https://untrusted.example.com/o";
    private static final String JWK_SET_URI = VALID_ISSUER + "/jwks";

    private SecurityConfiguration config;

    @BeforeEach
    void setUp() {
        config = new SecurityConfiguration(mock(ServiceAuthFilter.class));
    }

    @Test
    void decoder_created() {
        JwtDecoder result = config.jwtDecoder(
            resourceServerProperties(),
            idamSecurityProperties()
        );

        assertThat(result).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void valid_token() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
            .jwtValidator(List.of(VALID_ISSUER))
            .validate(jwtWithIssuer(VALID_ISSUER, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void invalid_token() {
        OAuth2TokenValidatorResult invalidIssuerResult = SecurityConfiguration
            .jwtValidator(List.of(VALID_ISSUER))
            .validate(jwtWithIssuer(INVALID_ISSUER, Instant.now().plusSeconds(60)));

        OAuth2TokenValidatorResult expiredTokenResult = SecurityConfiguration
            .jwtValidator(List.of(VALID_ISSUER))
            .validate(jwtWithIssuer(VALID_ISSUER, Instant.now().minusSeconds(120)));

        assertThat(invalidIssuerResult.hasErrors()).isTrue();
        assertThat(expiredTokenResult.hasErrors()).isTrue();
    }

    @Test
    void missing_token() {
        assertThatThrownBy(() -> SecurityConfiguration
            .jwtValidator(List.of(VALID_ISSUER))
            .validate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private OAuth2ResourceServerProperties resourceServerProperties() {
        OAuth2ResourceServerProperties resourceServerProperties = new OAuth2ResourceServerProperties();
        resourceServerProperties.getJwt().setJwkSetUri(JWK_SET_URI);
        return resourceServerProperties;
    }

    private IdamSecurityProperties idamSecurityProperties() {
        IdamSecurityProperties idamSecurityProperties = new IdamSecurityProperties();
        idamSecurityProperties.setAllowedIssuers(List.of(VALID_ISSUER));
        return idamSecurityProperties;
    }

    private Jwt jwtWithIssuer(String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);

        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("iss", issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
