package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchDarklyFeatureTogglerTest {

    @Mock
    private LDClientInterface ldClient;


    @InjectMocks
    private LaunchDarklyFeatureToggler launchDarklyFeatureToggler;

    @Test
    void should_return_default_value_when_key_does_not_exist() {
        String notExistingKey = "not-existing-key";

        when(ldClient.boolVariation(
            notExistingKey,
            null,
            true)
        ).thenReturn(true);

        assertTrue(launchDarklyFeatureToggler.getValue(notExistingKey, true));
    }

    @Test
    void should_return_value_when_key_exists() {
        String existingKey = "existing-key";
        when(ldClient.boolVariation(
            existingKey,
            null,
            false)
        ).thenReturn(true);

        assertTrue(launchDarklyFeatureToggler.getValue(existingKey, false));
    }
}
