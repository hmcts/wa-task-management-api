package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.NON_EXISTENT_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.PRIVILEGED_ACCESS_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.TEST_KEY;

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
        assertThat(launchDarklyFeature, is(false));
    }

    @Test
    public void should_hit_launch_darkly_for_privileged_access_feature_and_return_either_true_or_false() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(
            PRIVILEGED_ACCESS_FEATURE, SOME_USER_ID,  SOME_USER_EMAIL);
        assertThat(launchDarklyFeature, either(equalTo(true)).or(equalTo(false)));
    }

    @Test
    public void should_return_either_true_or_false_for_release_2_cancellation_completion_feature() {
        boolean launchDarklyFeature = featureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            SOME_USER_ID,
            SOME_USER_EMAIL
        );
        assertThat(launchDarklyFeature, either(equalTo(true)).or(equalTo(false)));
    }
}
