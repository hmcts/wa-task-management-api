package uk.gov.hmcts.reform.wataskmanagementapi.config;

import jakarta.annotation.PostConstruct;
import org.awaitility.Awaitility;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@TestConfiguration
@Profile("functional")
public class AwaitilityTestConfig {
    @PostConstruct
    public void configureAwaitility() {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(60));
        Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
        Awaitility.setDefaultPollDelay(Duration.ZERO);
    }
}
