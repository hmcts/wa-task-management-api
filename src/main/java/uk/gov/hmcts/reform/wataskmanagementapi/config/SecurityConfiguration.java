package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@ConfigurationProperties(prefix = "security")
@EnableWebSecurity
public class SecurityConfiguration {

    public static final String AUTHORIZATION = "Authorization";
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final List<String> anonymousPaths = new ArrayList<>();
    private final ServiceAuthFilter serviceAuthFiler;


    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
            "/swagger-ui.html",
            "/webjars/springfox-swagger-ui/**",
            "/swagger-resources/**",
            "/health",
            "/health/liveness",
            "/health/readiness",
            "/v2/api-docs/**",
            "/info",
            "/favicon.ico",
            "/"
        );
    }

    @Autowired
    public SecurityConfiguration(final ServiceAuthFilter serviceAuthFiler) {
        super();
        this.serviceAuthFiler = serviceAuthFiler;
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) {
        try {
            http
                .addFilterBefore(serviceAuthFiler, AbstractPreAuthenticatedProcessingFilter.class)
                .sessionManagement().sessionCreationPolicy(STATELESS)
                .and()
                .exceptionHandling()
                .and()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .authorizeRequests()
                .requestMatchers("/task-configuration/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/task/{\\\\d+}").permitAll()
                .requestMatchers(HttpMethod.POST, "/task/{\\\\d+}/initiation").permitAll()
                .requestMatchers(HttpMethod.POST, "/task/{\\\\d+}/notes").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/task/{\\\\d+}").permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .and()
                .and()
                .oauth2Client();
            return http.build();
        } catch (Exception e) {
            throw new ServerErrorException("Problem loading security config", e);
        }
    }

    public void configure(WebSecurity web) {
        web.ignoring().requestMatchers(anonymousPaths.toArray(String[]::new));
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp);

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    public List<String> getAnonymousPaths() {
        return anonymousPaths;
    }
}
