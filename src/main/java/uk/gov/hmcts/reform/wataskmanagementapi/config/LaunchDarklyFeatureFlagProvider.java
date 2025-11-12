package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import static java.util.Objects.requireNonNull;

@Slf4j
@Service
public class LaunchDarklyFeatureFlagProvider {

    private final LDClientInterface ldClient;
    public static final LDUser TM_USER = new LDUser.Builder("wa-task-management-api")
        .anonymous(true)
        .build();

    public LaunchDarklyFeatureFlagProvider(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getBooleanValue(FeatureFlag featureFlag, String userId, String email) {
        requireNonNull(featureFlag, "featureFlag must not be null");
        requireNonNull(userId, "userId must not be null");
        log.debug("Attempting to retrieve feature flag '{}' with email '{}'",
                  featureFlag.getKey(), email);
        boolean result =  ldClient.boolVariation(
            featureFlag.getKey(),
            createLaunchDarklyContext(userId, email),
            true);
        log.info("Feature flag '{}' has evaluated to '{}'", featureFlag.getKey(), result);
        log.info("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-04 2025-11-12T15:01:00Z");
        log.warn("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-05 2025-11-12T15:01:00Z");
        log.error("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-06 2025-11-12T15:01:00Z");
        log.error("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-06 2025-11-12T14:01:00Z");
        log.error("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-06 2025-11-12T16:01:00Z");

        log.error("TASK_INITIATION_FAILURES There are some uninitiated tasks lars-test-02 2025-11-12T15:01:00Z");
        log.error("FIND_PROBLEM_MESSAGES Retrieved problem messages lars-test-02 2025-11-12T15:01:00Z");
        log.error("Task Execute Reconfiguration Failed lars-test-02 2025-11-12T15:01:00Z");
        log.error("TASK_REPLICATION_ERROR: lars-test-02 2025-11-12T15:01:00Z");

        return result;
    }

    public LDValue getJsonValue(FeatureFlag featureFlag, LDValue defaultValue) {
        requireNonNull(featureFlag, "featureFlag must not be null");
        log.debug("Attempting to retrieve feature flag '{}'", featureFlag.getKey());
        LDValue result =  ldClient.jsonValueVariation(
            featureFlag.getKey(),
            LDContext.fromUser(TM_USER),
            defaultValue
        );
        log.info("Feature flag '{}' has evaluated to '{}'", featureFlag.getKey(), result);
        return result;
    }

    private LDContext createLaunchDarklyContext(String userId, String email) {
        return LDContext.builder("wa-task-management-api")
            .set("name", userId)
            .set("email", email)
            .set("firstName", "Work Allocation")
            .set("lastName", "Task Management")
            .build();
    }

}
