package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import jakarta.annotation.PostConstruct;
import org.awaitility.Awaitility;
import org.opentest4j.AssertionFailedError;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@TestConfiguration
@Profile({"integration","replica"})
public class AwaitilityIntegrationTestConfig {
    @PostConstruct
    public void configureAwaitility() {
        Awaitility.ignoreExceptionByDefault(AssertionFailedError.class);
        Awaitility.setDefaultTimeout(Duration.ofSeconds(90));
        Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
        Awaitility.setDefaultPollDelay(Duration.ZERO);
    }
}
