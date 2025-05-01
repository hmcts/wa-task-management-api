package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.LDValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.MANDATORY_FIELDS_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.NON_EXISTENT_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.TEST_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.WA_UPDATE_COMPLETION_PROCESS;


public class LaunchDarklyFeatureFlagProviderTest extends SpringBootFunctionalBaseTest {

    public static final String SOME_USER_ID = "some user id";
    public static final String SOME_USER_EMAIL = "test@test.com";

    @Autowired
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Test
    public void should_hit_launch_darkly_and_return_true() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(TEST_KEY, SOME_USER_ID, SOME_USER_EMAIL);
        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_default_value_for_boolean() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(
            NON_EXISTENT_KEY, SOME_USER_ID,  SOME_USER_EMAIL);
        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_and_return_jsonvalue() {
        LDValue launchDarklyFeature = featureFlagProvider.getJsonValue(MANDATORY_FIELDS_KEY,
                                                                       LDValue.of("jurisdictions"));
        assertThat(launchDarklyFeature, is(LDValue.of("jurisdictions")));
    }

    @Test
    public void should_hit_launch_darkly_and_return_jsonvalue_update_completion_process_flag() {
        boolean result = false;

        LDValue flagValue = featureFlagProvider.getJsonValue(
            WA_UPDATE_COMPLETION_PROCESS,
            LDValue.ofNull()
        );
        if (flagValue != null && flagValue.get("local") != null
            && flagValue.get("local").getType() == LDValueType.BOOLEAN) {
            result = flagValue.get("local").booleanValue();
        }
        assertThat(result, is(true));
    }
}
