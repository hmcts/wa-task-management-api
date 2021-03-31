package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LaunchDarklyFunctionalTest extends SpringBootFunctionalBaseTest {

    @Autowired
    private LaunchDarklyClient launchDarklyClient;

    @Test
    public void should_hit_launch_darkly_and_return_true() {
        boolean launchDarklyFeature = launchDarklyClient.getKey("tester");

        assertThat(launchDarklyFeature, is(true));
    }

    @Test
    public void should_hit_launch_darkly_with_non_existent_key_and_return_false() {
        boolean launchDarklyFeature = launchDarklyClient.getKey("non-existent");

        assertThat(launchDarklyFeature, is(false));
    }
}
