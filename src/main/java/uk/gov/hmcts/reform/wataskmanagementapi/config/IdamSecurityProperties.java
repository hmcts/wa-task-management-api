package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "idam.security")
public class IdamSecurityProperties {

    private List<String> allowedIssuers = new ArrayList<>();
    private boolean allowedIssuersValidatorEnabled = true;
}
