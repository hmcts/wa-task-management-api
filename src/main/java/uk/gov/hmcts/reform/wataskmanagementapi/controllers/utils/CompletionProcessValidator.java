package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.LDValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CompletionProcessValidator {

    private static final List<String> VALID_COMPLETION_PROCESS = Arrays.asList(
        "EXUI_USER_COMPLETION",
        "EXUI_CASE-EVENT_COMPLETION"
    );

    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Value("${environment}")
    private String environment;

    public CompletionProcessValidator(LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.featureFlagProvider = featureFlagProvider;
    }

    /**
     Checks if the "update completion process" feature flag is enabled for the current environment.

     The method retrieves the value of the feature flag from LaunchDarkly using the
     LaunchDarklyFeatureFlagProvider. It evaluates the flag value for the current environment
     (e.g., "local", "demo", "production") and ensures it is a valid boolean.

     If the flag value is not a valid boolean or is missing for the current environment,
     the method logs a warning and defaults to false.

     @return true if the feature flag is enabled for the current environment, false otherwise. */
    public boolean isUpdateCompletionProcessFlagEnabled() {
        LDValue flagValue = featureFlagProvider.getJsonValue(
            FeatureFlag.WA_UPDATE_COMPLETION_PROCESS,
            LDValue.ofNull()
        );
        if (flagValue != null && flagValue.get(environment) != null
            && flagValue.get(environment).getType() == LDValueType.BOOLEAN) {
            boolean result = flagValue.get(environment).booleanValue();
            log.info("Flag '{}' for environment '{}' evaluated to '{}'",
                     FeatureFlag.WA_UPDATE_COMPLETION_PROCESS, environment, result);
            return result;
        } else {
            log.warn("Flag '{}' does not contain a valid boolean for environment '{}', defaulting to '{}'",
                     FeatureFlag.WA_UPDATE_COMPLETION_PROCESS, environment, false);
            return false;
        }

    }

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
    public Optional<String> validate(String completionProcess, String taskId) {
        if (!isUpdateCompletionProcessFlagEnabled()) {
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
