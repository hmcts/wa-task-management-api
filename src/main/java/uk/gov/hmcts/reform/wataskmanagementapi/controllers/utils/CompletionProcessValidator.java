package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

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
     * Validation logic:
     *      * If the updateCompletionProcessFlagEnabled flag is disabled, the method logs an info message
     *      * and returns an empty {@link Optional}.
     *      * If the completion process is null, blank, or not in the list of valid completion processes,
     *      * the method logs a warning and returns an empty {@link Optional}.
     *      * If the completion process is valid, the method logs an info message and returns the completion process
     *      * wrapped in an {@link Optional}.
     *
     * @param completionProcess the completion process value to validate
     * @param taskId the task ID for logging purposes
     * @return an Optional containing the valid completion process value, or empty if invalid.
     */
    public Optional<String> validate(String completionProcess, String taskId,
                                     AccessControlResponse accessControlResponse) {
        if (!isCompletionProcessFeatureEnabled(accessControlResponse)) {
            log.info("Update completion process flag is disabled. No action taken for task with id {}", taskId);
            return Optional.empty();
        } else if (completionProcess == null || completionProcess.isBlank()
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

    public boolean isCompletionProcessFeatureEnabled(AccessControlResponse accessControlResponse) {
        return launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.WA_COMPLETION_PROCESS_UPDATE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );
    }

}
