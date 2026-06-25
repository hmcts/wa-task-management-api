package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "config")
@Getter
@Setter
public class ClientAccessProperties {

    private List<String> privilegedAccessClients = List.of();
    private List<String> exclusiveAccessClients = List.of();
    private Map<String, List<String>> serviceCaseTypeAccess = Map.of();
}
