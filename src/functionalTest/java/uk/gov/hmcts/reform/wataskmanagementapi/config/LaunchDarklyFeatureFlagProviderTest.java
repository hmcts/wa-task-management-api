package uk.gov.hmcts.reform.wataskmanagementapi.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.NON_EXISTENT_KEY;
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
        assertThat(launchDarklyFeature, is(true));
    }
    @Test
    public void should_hit_launch_darkly_and_return_jsonvalue() {
        LDValue launchDarklyFeature = featureFlagProvider.getJSONValue(TEST_KEY, SOME_USER_ID, SOME_USER_EMAIL, LDValue.of("jurisdictions"));
        assertThat(launchDarklyFeature, is(LDValue.of("jurisdictions")));
    }
}
