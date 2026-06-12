package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class CancellationProcessValidator {

    private static final List<String> VALID_CANCELLATION_PROCESS = Arrays.asList(
        "EXUI_USER_CANCELLATION",
        "CASE_EVENT_CANCELLATION"
    );

    /**
     * Validates the cancellation process value.
     *
     * This method checks whether the provided `cancellationProcess` value is valid based on predefined criteria.
     * The validation process includes:
     * - Checking if the `cancellationProcess` is null, blank, or not part of the valid cancellation processes.
     * - Logging appropriate messages based on the validation outcome.
     * - Returning an `Optional` containing the valid `cancellationProcess` value, or an empty `Optional` if invalid.
     *
     * Validation steps:
     * 1. If `cancellationProcess` is null, blank, or not in the list of valid cancellation processes:
     *    - Logs a warning message indicating the invalid value and task ID.
     *    - Returns an empty `Optional`.
     * 2. If `cancellationProcess` is valid:
     *    - Logs an info message indicating the valid value and task ID.
     *    - Returns the `cancellationProcess` wrapped in an `Optional`.
     *
     * @param cancellationProcess the cancellation process value to validate
     * @param taskId the task ID for logging purposes
     * @return an `Optional` containing the valid cancellation process value, or empty if invalid
     */
    public Optional<String> validate(String cancellationProcess, String taskId) {
        if (cancellationProcess == null || cancellationProcess.isBlank()
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
