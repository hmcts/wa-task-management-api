package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CancellationProcessValidatorTest {

    private CancellationProcessValidator cancellationProcessValidator;


    @BeforeEach
    void setUp() {
        cancellationProcessValidator = new CancellationProcessValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_CANCELLATION", "EXUI_CASE_EVENT_CANCELLATION"})
    void validate_returns_completion_process_when_valid(String validCompletionProcess) {
        Optional<String> result = cancellationProcessValidator.validate(validCompletionProcess, "taskId123", true);
        assertTrue(result.isPresent());
        assertEquals(validCompletionProcess, result.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INVALID_PROCESS", "RANDOM_VALUE"})
    void validate_returns_empty_for_invalid_or_blank_completion_process(String invalidCompletionProcess) {
        Optional<String> result = cancellationProcessValidator.validate(invalidCompletionProcess, "taskId123", true);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_CANCELLATION", "EXUI_CASE_EVENT_CANCELLATION"})
    void validate_returns_empty_when_update_completion_process_flag_is_disabled(String validCompletionProcess) {

        Optional<String> result = cancellationProcessValidator.validate(validCompletionProcess, "taskId123", false);
        assertTrue(result.isEmpty());
    }
}
