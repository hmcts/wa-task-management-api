package uk.gov.hmcts.reform.wataskmanagementapi.config;

import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.NON_EXISTENT_KEY;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.TEST_KEY;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
@Slf4j
public class LaunchDarklyFeatureFlagProviderTest {

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
}
