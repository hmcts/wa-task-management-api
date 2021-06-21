package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchDarklyFeatureFlagProviderTest {

    @Mock
    private LDClientInterface ldClient;

    @InjectMocks
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private LDUser expectedLdUser;

    @BeforeEach
    void setup() {

        expectedLdUser = new LDUser.Builder("wa-task-management-api")
            .name("some user id")
            .firstName("Work Allocation")
            .lastName("Task Management")
            .build();
    }

    @ParameterizedTest
    @CsvSource({
        "false, true, true",
        "false, false, false"
    })
    void getBooleanValue_return_expectedFlagValue(
        boolean defaultValue,
        boolean boolVariationReturn,
        boolean expectedFlagValue
    ) {
        when(ldClient.boolVariation(FeatureFlag.TEST_KEY.getKey(), expectedLdUser, defaultValue))
            .thenReturn(boolVariationReturn);

        assertThat(launchDarklyFeatureFlagProvider.getBooleanValue(FeatureFlag.TEST_KEY, "some user id"))
            .isEqualTo(expectedFlagValue);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, some user id, featureFlag must not be null",
        "PRIVILEGED_ACCESS_FEATURE, NULL, userId must not be null"}, nullValues = "NULL")
    void getBooleanValue_edge_case_scenarios(FeatureFlag featureFlag, String userId, String expectedMessage) {
        assertThatThrownBy(() -> launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag, userId))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

}
