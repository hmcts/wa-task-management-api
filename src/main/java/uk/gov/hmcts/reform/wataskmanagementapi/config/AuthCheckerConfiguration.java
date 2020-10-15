package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.google.common.collect.ImmutableSet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.servlet.http.HttpServletRequest;

@Configuration
@ConfigurationProperties(prefix = "security")
public class AuthCheckerConfiguration {
    private final List<String> authorisedServices = new ArrayList<>();

    public List<String> getAuthorisedServices() {
        return authorisedServices;
    }


    @Bean
    public Function<HttpServletRequest, Collection<String>> authorizedServicesExtractor() {
        return any -> ImmutableSet.copyOf(authorisedServices);
    }

    @Bean
    public Function<HttpServletRequest, Optional<String>> userIdExtractor() {
        return any -> Optional.empty();
    }
}
