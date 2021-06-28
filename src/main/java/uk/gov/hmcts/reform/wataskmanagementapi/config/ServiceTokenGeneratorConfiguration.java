package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

@Configuration
@Lazy
public class ServiceTokenGeneratorConfiguration {

    @Bean
    public AuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.secret}") String secret,
        @Value("${idam.s2s-auth.name}") String microService,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(
            secret,
            microService,
            serviceAuthorisationApi
        );
    }

    @Bean
    public ServiceAuthTokenValidator serviceAuthTokenValidator(
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return new ServiceAuthTokenValidator(serviceAuthorisationApi);
    }
}
