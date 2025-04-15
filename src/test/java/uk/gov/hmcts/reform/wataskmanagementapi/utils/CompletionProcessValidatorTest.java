package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CompletionProcessValidator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletionProcessValidatorTest {

    private CompletionProcessValidator completionProcessValidator;

    @BeforeEach
    void setUp() {
        completionProcessValidator = new CompletionProcessValidator();
        ReflectionTestUtils.setField(completionProcessValidator, "updateCompletionProcessFlagEnabled", true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"})
    void validate_returns_completion_process_when_valid(String validCompletionProcess) {
        Optional<String> result = completionProcessValidator.validate(validCompletionProcess, "taskId123");
        assertTrue(result.isPresent());
        assertEquals(validCompletionProcess, result.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INVALID_PROCESS", "RANDOM_VALUE"})
    void validate_returns_empty_for_invalid_or_blank_completion_process(String invalidCompletionProcess) {
        Optional<String> result = completionProcessValidator.validate(invalidCompletionProcess, "taskId123");
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_COMPLETION", "EXUI_CASE-EVENT_COMPLETION"})
    void validate_returns_empty_when_update_completion_process_flag_is_disabled(String validCompletionProcess) {
        ReflectionTestUtils.setField(completionProcessValidator, "updateCompletionProcessFlagEnabled", false);
        Optional<String> result = completionProcessValidator.validate(validCompletionProcess, "taskId123");
        assertTrue(result.isEmpty());
    }
}
