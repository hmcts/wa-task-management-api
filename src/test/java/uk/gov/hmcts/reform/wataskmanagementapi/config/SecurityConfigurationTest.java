package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.TaskInitiationController.class, properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwks"
})
@Import({SecurityConfiguration.class, IdamSecurityProperties.class, SecurityConfigurationTest.SecurityTestConfig.class})
class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String INVALID_ISSUER = "https://untrusted.example.com/o";
    private static final String JWK_SET_URI = VALID_ISSUER + "/jwks";
    private static final Instant VALID_EXPIRY = Instant.parse("2099-01-01T00:00:00Z");
    private static final Instant EXPIRED = Instant.parse("2000-01-01T00:00:00Z");

    private SecurityConfiguration config;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        config = new SecurityConfiguration(mock(ServiceAuthFilter.class));
    }

    @Test
    void jwtDecoderCreated() {
        JwtDecoder result = config.jwtDecoder(
                JWK_SET_URI,
                idamSecurityProperties()
        );

        assertThat(result).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    void jwtValidatorWithValidIssuer() {
        OAuth2TokenValidatorResult result = SecurityConfiguration
                .jwtValidator(List.of(VALID_ISSUER))
                .validate(jwtWithIssuer(VALID_ISSUER, VALID_EXPIRY));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void jwtValidatorWithInvalidIssuer() {
        OAuth2TokenValidatorResult invalidIssuerResult = SecurityConfiguration
                .jwtValidator(List.of(VALID_ISSUER))
                .validate(jwtWithIssuer(INVALID_ISSUER, VALID_EXPIRY));

        assertThat(invalidIssuerResult.hasErrors()).isTrue();
    }

    @Test
    void jwtValidatorWithExpiredIssuer() {
        OAuth2TokenValidatorResult expiredTokenResult = SecurityConfiguration
                .jwtValidator(List.of(VALID_ISSUER))
                .validate(jwtWithIssuer(VALID_ISSUER, EXPIRED));

        assertThat(expiredTokenResult.hasErrors()).isTrue();
    }

    @Test
    void jwtIssuerMissing() {
        OAuth2TokenValidator<Jwt> validator =
                SecurityConfiguration.allowedIssuersValidator(List.of(VALID_ISSUER));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();

        assertTrue(validator.validate(jwt).hasErrors());
    }

    @Test
    void taskOperationPostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/operation"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void taskDeletePostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/delete"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void searchForCompletablePostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/task/search-for-completable"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void taskInitiationPostIsPermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/task/123e4567-e89b-12d3-a456-426614174000/initiation"))
            .andExpect(status().isNoContent());
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        ServiceAuthFilter serviceAuthFilter() {
            return mock(ServiceAuthFilter.class);
        }
    }

    @RestController
    static class TaskInitiationController {

        @PostMapping("/task/{taskId}/initiation")
        @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
        void initiate(@PathVariable("taskId") String taskId) {
            // An empty handler lets the test distinguish a permitted request from an unauthorized one.
        }
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
