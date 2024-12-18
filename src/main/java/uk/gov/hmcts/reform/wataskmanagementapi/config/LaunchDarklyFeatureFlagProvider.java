package uk.gov.hmcts.reform.wataskmanagementapi.config;

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
            createLaunchDarklyUser(userId, email),
            true);
        log.info("Feature flag '{}' has evaluated to '{}'", featureFlag.getKey(), result);
        return result;
    }

    public LDValue getJsonValue(FeatureFlag featureFlag, LDValue defaultValue) {
        requireNonNull(featureFlag, "featureFlag must not be null");
        log.debug("Attempting to retrieve feature flag '{}'", featureFlag.getKey());
        LDValue result =  ldClient.jsonValueVariation(
            featureFlag.getKey(),
            TM_USER,
            defaultValue
        );
        log.info("Feature flag '{}' has evaluated to '{}'", featureFlag.getKey(), result);
        return result;
    }

    private LDUser createLaunchDarklyUser(String userId, String email) {
        return new LDUser.Builder("wa-task-management-api")
            .name(userId)
            .email(email)
            .firstName("Work Allocation")
            .lastName("Task Management")
            .build();
    }

}
