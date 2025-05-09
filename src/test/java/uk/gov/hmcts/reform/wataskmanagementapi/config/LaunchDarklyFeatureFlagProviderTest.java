package uk.gov.hmcts.reform.wataskmanagementapi.config;

import com.launchdarkly.sdk.LDContext;
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

    private LDContext expectedLdContext;

    @BeforeEach
    void setup() {
        expectedLdContext = LDContext.builder("wa-task-management-api")
            .set("name", "some user id")
            .set("email", "test@test.com")
            .set("firstName", "Work Allocation")
            .set("lastName", "Task Management")
            .build();
    }

    @ParameterizedTest
    @CsvSource({
        "true, true, true",
        "true, false, false"
    })
    void getBooleanValue_return_expectedFlagValue(
        boolean defaultValue,
        boolean boolVariationReturn,
        boolean expectedFlagValue
    ) {
        when(ldClient.boolVariation(FeatureFlag.TEST_KEY.getKey(), expectedLdContext, defaultValue))
            .thenReturn(boolVariationReturn);

        assertThat(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.TEST_KEY, "some user id", "test@test.com"))
            .isEqualTo(expectedFlagValue);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "NULL, some user id, test@test.com, featureFlag must not be null",
        "WA_TASK_SEARCH_GIN_INDEX, NULL, test@test.com, userId must not be null"}, nullValues = "NULL")
    void getBooleanValue_edge_case_scenarios(
        FeatureFlag featureFlag, String userId, String email, String expectedMessage
    ) {
        assertThatThrownBy(() -> launchDarklyFeatureFlagProvider.getBooleanValue(featureFlag, userId, email))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(expectedMessage);
    }

}
