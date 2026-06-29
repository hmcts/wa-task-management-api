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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CompletionProcessValidator;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CompletionProcessValidatorTest {

    private CompletionProcessValidator completionProcessValidator;

    AccessControlResponse mockAccessControlResponse;
    @Mock
    private RoleAssignment mockedRoleAssignment;

    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @BeforeEach
    void setUp() {
        completionProcessValidator = new CompletionProcessValidator(launchDarklyFeatureFlagProvider);
        mockAccessControlResponse = new AccessControlResponse(
            new UserInfo("id", "idamId", List.of("Admin"), "surname", "email", "familyName"),
            singletonList(mockedRoleAssignment)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"})
    void should_return_completion_process_when_valid_value_passed_for_validation(String validCompletionProcess) {
        Optional<String> result =
            completionProcessValidator.validate(validCompletionProcess, "taskId123");
        assertTrue(result.isPresent());
        assertEquals(validCompletionProcess, result.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INVALID_PROCESS", "RANDOM_VALUE"})
    void should_return_empty_completion_process_when_invalid_or_blank_value_passed(String invalidCompletionProcess) {
        Optional<String> result =
            completionProcessValidator.validate(invalidCompletionProcess, "taskId123");
        assertTrue(result.isEmpty());
    }
}
