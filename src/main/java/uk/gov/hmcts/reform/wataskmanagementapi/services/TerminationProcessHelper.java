package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.util.Optional;

@AllArgsConstructor
@Getter
@Slf4j
@Component
public class TerminationProcessHelper {
    private CamundaService camundaService;
    private IdamTokenGenerator idamTokenGenerator;
    private CancellationProcessValidator cancellationProcessValidator;


    /**
     * Fetches the cancellation process history variable for a given task ID.
     * This method queries the Camunda service to retrieve the value of the
     * "cancellationProcess" variable from the task's history.
     *
     * @param taskId The ID of the task for which the cancellation process variable is to be fetched.
     * @return An Optional containing the value of the "cancellationProcess" variable if it exists,
    or an empty Optional if the variable is not found.
     */
    private Optional<String> fetchCancellationProcessHistoryVar(String taskId) {
        return camundaService.getVariableFromHistory(taskId, "cancellationProcess")
            .map(HistoryVariableInstance::getValue)
            .map(value -> {
                log.debug(
                    "Task {} had cancellationProcess='{}' in Camunda history",
                    taskId,
                    value
                );
                return value;
            })
            .or(() -> {
                log.debug(
                    "Task {} did not have cancellationProcess variable in Camunda history",
                    taskId
                );
                return Optional.empty();
            });
    }

    /**
     * Fetches the termination process for a given task ID from Camunda.
     * This method retrieves the termination process for a specific task by checking the
     * "cancellationProcess" variable in the task's history within Camunda. It validates
     * the retrieved value and ensures it corresponds to a valid TerminationProcess.
     * The process involves:
     * - Fetching the "cancellationProcess" variable from the task's history.
     * - Validating the fetched value to ensure it matches a valid TerminationProcess.
     * - Returning the corresponding TerminationProcess if valid, or an empty Optional otherwise.
     *
     * @param taskId The unique identifier of the task for which the termination process is to be fetched.
     * @return An Optional containing the TerminationProcess if the "cancellationProcess" variable is valid
     *         and found in the task's history, or an empty Optional if not.
     */
    public Optional<TerminationProcess> fetchTerminationProcessFromCamunda(String taskId) {

        return getValidatedTerminationProcess(
            taskId,
            fetchCancellationProcessHistoryVar(taskId)
            );

    }

    /**
     * Validates and retrieves the TerminationProcess for a given task.
     * This method checks if the cancellation process is present and valid for the given task ID.
     * If the cancellation process is valid, it maps it to a TerminationProcess enum value.
     * The validation is performed using the CancellationProcessValidator.
     * @param taskId             The unique identifier of the task for which the termination process is to be validated.
     * @param cancellationProcessOpt An Optional containing the cancellation process string, if present.
     * @return An Optional containing the TerminationProcess if the cancellation process is valid,
     *         or an empty Optional if the cancellation process is invalid or not present.
     */
    private Optional<TerminationProcess> getValidatedTerminationProcess(String taskId,
                                                                        Optional<String> cancellationProcessOpt) {
        if (cancellationProcessOpt.isPresent()) {
            String cancellationProcess = cancellationProcessOpt.get();
            if (cancellationProcessValidator.validate(cancellationProcess, taskId).isPresent()) {
                return TerminationProcess.fromValue(cancellationProcess);
            } else {
                log.warn(
                    "Task {} has invalid cancellationProcess='{}' in Camunda history",
                    taskId,
                    cancellationProcess
                );
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the termination process for a task during termination.
     * this method fetches the termination process from Camunda and sets it on the task.
     *
     * @param taskId        The ID of the task being terminated.
     * @param task          The task resource to update with the termination process.
     */
    public void setTerminationProcessOnTerminateTask(String taskId, TaskResource task) {
        this.fetchTerminationProcessFromCamunda(taskId).ifPresent(terminationProcess -> {
            CFTTaskState state = task.getState();
            if (state == CFTTaskState.COMPLETED || state == CFTTaskState.TERMINATED) {
                return;
            }

            if (state == CFTTaskState.CANCELLED || task.getTerminationProcess() != null) {
                log.warn("Cannot update the termination process for a Case Event Cancellation since it has"
                             + " already been cancelled by a User for task {}", taskId);
                return;
            }


            task.setTerminationProcess(terminationProcess);
        });
    }
}
