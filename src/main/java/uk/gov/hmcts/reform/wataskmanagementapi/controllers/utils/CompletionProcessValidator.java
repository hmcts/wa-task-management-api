package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class CompletionProcessValidator {

    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private static final List<String> VALID_COMPLETION_PROCESS = Arrays.asList(
        "EXUI_USER_COMPLETION",
        "EXUI_CASE-EVENT_COMPLETION"
    );

    /**
     * Validates the completion process value.
     * This method ensures that the provided `completionProcess` value is valid based on predefined criteria.
     * The validation logic includes:
     * - Checking if the `completionProcess` is null, blank, or not part of the valid completion processes.
     * - Logging appropriate messages based on the validation outcome.
     * - Returning an `Optional` containing the valid `completionProcess` value, or an empty `Optional` if invalid.
     * Validation steps:
     * 1. If `completionProcess` is null, blank, or not in the list of valid completion processes:
     *    - Logs a warning message indicating the invalid value and task ID.
     *    - Returns an empty `Optional`.
     * 2. If `completionProcess` is valid:
     *    - Logs an info message indicating the valid value and task ID.
     *    - Returns the `completionProcess` wrapped in an `Optional`.
     *
     * @param completionProcess the completion process value to validate
     * @param taskId the task ID for logging purposes
     * @return an `Optional` containing the valid completion process value, or empty if invalid
     */
    public Optional<String> validate(String completionProcess, String taskId) {
        if (completionProcess == null || completionProcess.isBlank()
            || !VALID_COMPLETION_PROCESS.contains(completionProcess)) {
            log.warn("Invalid CompletionProcess value: {} was received and no action was taken for task with id {}",
                     completionProcess, taskId);
            return Optional.empty();
        } else {
            log.info("CompletionProcess value: {} was received and updating in database for task with id {}",
                     completionProcess, taskId);
            return Optional.of(completionProcess);
        }
    }



}
