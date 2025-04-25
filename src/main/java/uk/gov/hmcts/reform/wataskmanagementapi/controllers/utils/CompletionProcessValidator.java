package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CompletionProcessValidator {

    @Value("${config.updateCompletionProcessFlagEnabled}")
    private boolean updateCompletionProcessFlagEnabled;

    private static final List<String> VALID_COMPLETION_PROCESS = Arrays.asList(
        "EXUI_USER_COMPLETION",
        "EXUI_CASE-EVENT_COMPLETION"
    );

    /**
     * Validates the completion process value.
     *
     * @param completionProcess the completion process value to validate
     * @param taskId the task ID for logging purposes
     * @return an Optional containing the valid completion process value, or empty if invalid
     * Validation logic:
     * If the updateCompletionProcessFlagEnabled flag is disabled, the method logs an info message
     * and returns an empty {@link Optional}.
     * If the completion process is null, blank, or not in the list of valid completion processes,
     * the method logs a warning and returns an empty {@link Optional}.
     * If the completion process is valid, the method logs an info message and returns the completion process
     */
    public Optional<String> validate(String completionProcess, String taskId) {
        if (!updateCompletionProcessFlagEnabled) {
            log.info("Update completion process flag is disabled. No action taken for task with id {}", taskId);
            return Optional.empty();
        } else if (completionProcess == null || completionProcess.isBlank()
            || !VALID_COMPLETION_PROCESS.contains(completionProcess)) {
            log.warn("Invalid TerminationProcess value: {} was received and no action was taken for task with id {}",
                     completionProcess, taskId);
            return Optional.empty();
        } else {
            log.info("TerminationProcess value: {} was received and updating in database for task with id {}",
                     completionProcess, taskId);
            return Optional.of(completionProcess);
        }
    }
}
