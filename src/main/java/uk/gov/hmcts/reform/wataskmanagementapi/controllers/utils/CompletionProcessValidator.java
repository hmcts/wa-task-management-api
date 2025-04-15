package uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public Optional<String> validate(String completionProcess, String taskId) {
        if (completionProcess == null || completionProcess.isBlank()) {
            return Optional.empty();
        }

        if (VALID_COMPLETION_PROCESS.contains(completionProcess)) {
            log.info("TerminationProcess value: {} was received and updating in database for task with id {}",
                     completionProcess, taskId);
            return Optional.of(completionProcess);
        } else {
            log.warn("Invalid TerminationProcess value: {} was received and no action was taken for task with id {}",
                     completionProcess, taskId);
            return Optional.empty();
        }
    }
}
