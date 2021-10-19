package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

@Slf4j
@Service
@Profile("local")
public class LaunchDarklyLocalFeatureFlagProvider {

    private final LDClientInterface ldClient;

    public LaunchDarklyLocalFeatureFlagProvider(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    public boolean getBooleanValue(FeatureFlag featureFlag, String userId) {
        return false;
    }

}
