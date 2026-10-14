package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Slf4j
@Configuration
@Getter
@ConfigurationProperties(prefix = "security")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final List<String> anonymousPaths = new ArrayList<>();
    private final ServiceAuthFilter serviceAuthFiler;

    @Autowired
    public SecurityConfiguration(final ServiceAuthFilter serviceAuthFiler) {
        super();
        this.serviceAuthFiler = serviceAuthFiler;
    }

    @Bean
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .addFilterBefore(serviceAuthFiler, BearerTokenAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .exceptionHandling(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .formLogin(formLogin -> formLogin.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/task-configuration/**").permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.POST, "/task/\\d+")).permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.POST, "/task/\\d+/initiation")).permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.POST, "/task/\\d+/notes")).permitAll()
                .requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.DELETE, "/task/\\d+")).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .oauth2Client(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(anonymousPaths.toArray(String[]::new));
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
        IdamSecurityProperties idamSecurityProperties
    ) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri(jwkSetUri)
            .build();

        jwtDecoder.setJwtValidator(jwtValidator(idamSecurityProperties.getAllowedIssuers()));

        return jwtDecoder;
    }

    static OAuth2TokenValidator<Jwt> jwtValidator(List<String> allowedIssuers) {
        return new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            allowedIssuersValidator(allowedIssuers)
        );
    }

    static OAuth2TokenValidator<Jwt> allowedIssuersValidator(List<String> allowedIssuers) {
        Set<String> allowedIssuerSet = Set.copyOf(allowedIssuers);
        return new JwtClaimValidator<>("iss", issuer -> issuer != null && allowedIssuerSet.contains(issuer));
    }
}
