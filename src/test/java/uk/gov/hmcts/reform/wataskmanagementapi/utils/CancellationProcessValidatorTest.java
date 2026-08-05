package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
class CancellationProcessValidatorTest {

    @InjectMocks
    private CancellationProcessValidator cancellationProcessValidator;

    @ParameterizedTest
    @ValueSource(strings = {"EXUI_USER_CANCELLATION", "CASE_EVENT_CANCELLATION"})
    void should_return_cancellation_process_when_valid_value_passed_for_validation(String validCompletionProcess) {
        Optional<String> result =
            cancellationProcessValidator.validate(validCompletionProcess, "taskId123");
        assertTrue(result.isPresent());
        assertEquals(validCompletionProcess, result.get());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"INVALID_PROCESS", "RANDOM_VALUE"})
    void should_return_empty_cancellation_process_when_invalid_or_blank_value_passed(String invalidCompletionProcess) {

        Optional<String> result =
            cancellationProcessValidator.validate(invalidCompletionProcess, "taskId123");
        assertTrue(result.isEmpty());
    }

}
