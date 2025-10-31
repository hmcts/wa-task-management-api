package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CancellationProcessValidator {

    private static final List<String> VALID_CANCELLATION_PROCESS = Arrays.asList(
        "EXUI_USER_CANCELLATION",
        "EXUI_CASE_EVENT_CANCELLATION"
    );

    /**
     * Validates the cancellation process value.
     * Validation logic:
     *      * If the updateCancellationProcessFlagEnabled flag is disabled, the method logs an info message
     *      * and returns an empty {@link Optional}.
     *      * If the cancellation process is null, blank, or not in the list of valid completion processes,
     *      * the method logs a warning and returns an empty {@link Optional}.
     *      * If the cancellation process is valid, the method logs an info message and returns the cancellation process
     *      * wrapped in an {@link Optional}.
     *
     * @param cancellationProcess the cancellation process value to validate
     * @param taskId the task ID for logging purposes
     * @return an Optional containing the valid completion process value, or empty if invalid.
     */
    public Optional<String> validate(String cancellationProcess, String taskId,
                                     boolean updateCancellationProcessFlagEnabled) {
        if (!updateCancellationProcessFlagEnabled) {
            log.info("Update cancellation process flag is disabled. No action taken for task with id {}", taskId);
            return Optional.empty();
        } else if (cancellationProcess == null || cancellationProcess.isBlank()
            || !VALID_CANCELLATION_PROCESS.contains(cancellationProcess)) {
            log.warn("Invalid CancellationProcess value: {} was received and no action was taken for task with id {}",
                     cancellationProcess, taskId);
            return Optional.empty();
        } else {
            log.info("CancellationProcess value: {} was received and updating in database for task with id {}",
                     cancellationProcess, taskId);
            return Optional.of(cancellationProcess);
        }
    }
}
