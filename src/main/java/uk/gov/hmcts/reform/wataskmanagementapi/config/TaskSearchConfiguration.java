package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search")
public class TaskSearchConfiguration {

    private Map<String, String> jurisdictionUsers = new HashMap<>();
}
