package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDValue;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.MANDATORY_FIELDS_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.NON_EXISTENT_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.TEST_KEY;

public class LaunchDarklyFeatureFlagProviderTest extends SpringBootFunctionalBaseTest {

    public static final String SOME_USER_ID = "some user id";
    public static final String SOME_USER_EMAIL = "test@test.com";

    public static final String USER_WITH_COMPLETION_FLAG_ENABLED = "wa-user-with-completion-process-enabled";

    public static final String USER_WITH_COMPLETION_FLAG_DISABLED = "wa-user-with-completion-process-disabled";

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
    public void should_hit_launch_darkly_and_return_as_true_when_update_completion_process_flag_enabled_for_user() {
        boolean flagValue = featureFlagProvider.getBooleanValue(
            FeatureFlag.WA_COMPLETION_PROCESS_UPDATE, SOME_USER_EMAIL, USER_WITH_COMPLETION_FLAG_ENABLED);

        assertTrue(flagValue);
    }

    @Test
    public void should_hit_launch_darkly_and_return_as_true_when_update_completion_process_flag_disabled_for_user() {
        boolean flagValue = featureFlagProvider.getBooleanValue(
            FeatureFlag.WA_COMPLETION_PROCESS_UPDATE, SOME_USER_EMAIL, USER_WITH_COMPLETION_FLAG_DISABLED);

        assertFalse(flagValue);
    }
}
