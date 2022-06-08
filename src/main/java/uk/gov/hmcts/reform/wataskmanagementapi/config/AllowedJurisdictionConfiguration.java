package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "config")
@Getter
@Setter
public class AllowedJurisdictionConfiguration {
    List<String> allowedJurisdictions;
    List<String> allowedCaseTypes;
}

