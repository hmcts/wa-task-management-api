package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CancellationProcessValidatorTest {

    private CancellationProcessValidator cancellationProcessValidator;
    AccessControlResponse mockAccessControlResponse;
    @Mock
    private RoleAssignment mockedRoleAssignment;

    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @BeforeEach
    void setUp() {
        cancellationProcessValidator = new CancellationProcessValidator(launchDarklyFeatureFlagProvider);
        mockAccessControlResponse = new AccessControlResponse(
            new UserInfo("id", "idamId", List.of("Admin"), "surname", "email", "familyName"),
            singletonList(mockedRoleAssignment)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_CANCELLATION", "CASE_EVENT_CANCELLATION"})
    void should_return_cancellation_process_when_valid_value_passed_for_validation(String validCompletionProcess) {
        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_CANCELLATION_PROCESS_FEATURE),
                                                                       any(), anyString())).thenReturn(true);
        Optional<String> result =
            cancellationProcessValidator.validate(validCompletionProcess, "taskId123", mockAccessControlResponse);
        assertTrue(result.isPresent());
        assertEquals(validCompletionProcess, result.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INVALID_PROCESS", "RANDOM_VALUE"})
    void should_return_empty_cancellation_process_when_invalid_or_blank_value_passed(String invalidCompletionProcess) {
        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_CANCELLATION_PROCESS_FEATURE),
                                                                       any(), anyString())).thenReturn(true);

        Optional<String> result =
            cancellationProcessValidator.validate(invalidCompletionProcess, "taskId123", mockAccessControlResponse);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_CANCELLATION", "EXUI_CASE_EVENT_CANCELLATION"})
    void should_return_empty_cancellation_process_when_flag_is_disabled(String validCompletionProcess) {
        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_CANCELLATION_PROCESS_FEATURE),
                                                                       any(), anyString())).thenReturn(false);

        Optional<String> result =
            cancellationProcessValidator.validate(validCompletionProcess, "taskId123", mockAccessControlResponse);
        assertTrue(result.isEmpty());
    }
}
