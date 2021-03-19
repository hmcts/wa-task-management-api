package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.services.FeatureToggler;

@Service
public class LaunchDarklyFeatureToggler implements FeatureToggler {

    private final LDClientInterface ldClient;

    public LaunchDarklyFeatureToggler(LDClientInterface ldClient) {
        this.ldClient = ldClient;
    }

    @Override
    public boolean getValue(String key, Boolean defaultValue) {

        return ldClient.boolVariation(
            key,
          null,
            defaultValue
        );
    }

}
