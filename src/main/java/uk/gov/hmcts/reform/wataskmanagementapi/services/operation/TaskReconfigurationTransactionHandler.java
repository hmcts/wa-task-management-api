package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.MANDATORY_FIELD_MISSING_ERROR;

/**
 * Helper class for reconfiguring task resources.
 */
@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskReconfigurationTransactionHandler {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;
    private final TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    /**
     * Constructor for TaskReconfigurationTransactionHandler.
     *
     * @param cftTaskDatabaseService the CFT task database service
     * @param configureTaskService the configure task service
     * @param taskAutoAssignmentService the task auto-assignment service
     */
    public TaskReconfigurationTransactionHandler(CFTTaskDatabaseService cftTaskDatabaseService,
                                                 ConfigureTaskService configureTaskService,
                                                 TaskAutoAssignmentService taskAutoAssignmentService,
                                                 TaskMandatoryFieldsValidator taskMandatoryFieldsValidator) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
        this.taskMandatoryFieldsValidator = taskMandatoryFieldsValidator;
    }

    /**
     * Resets the indexed attribute of the given task resource.
     *
     * @param taskResource the task resource to reset
     */
    public void resetIndexed(TaskResource taskResource) {
        log.info("indexed attribute for the task (id={}) before change is {} ",
                 taskResource.getTaskId(), taskResource.getIndexed());
        if (!taskResource.getIndexed()
            && (taskResource.getState() == CFTTaskState.ASSIGNED
            || taskResource.getState() == CFTTaskState.UNASSIGNED)) {
            taskResource.setIndexed(true);
        }
        log.info("indexed attribute for the task (id={}) after change is {} taskResource {} ",
                 taskResource.getTaskId(), taskResource.getIndexed(), taskResource);
    }

    /**
     * Reconfigures the task resource with the given task ID.
     * This method runs in a new transaction and rolls back if any exception occurs.
     *
     * @param taskId the ID of the task to reconfigure
     * @return the reconfigured task resource, or null if the task could not be found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Optional<TaskResource> reconfigureTaskResource(String taskId) {
        Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
            .findByIdAndStateInObtainPessimisticWriteLock(taskId, List.of(
                CFTTaskState.ASSIGNED,
                CFTTaskState.UNASSIGNED
            ));
        if (optionalTaskResource.isPresent()) {
            TaskResource taskResource = optionalTaskResource.get();
            try {
                log.info("Re-configure task-id {}", taskId);
                taskResource = configureTaskService.reconfigureCFTTask(taskResource);
                taskMandatoryFieldsValidator.validate(taskResource);
                taskResource = taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);
                taskResource.setReconfigureRequestTime(null);
                taskResource.setLastReconfigurationTime(OffsetDateTime.now());
                resetIndexed(taskResource);
                return Optional.of(cftTaskDatabaseService.saveTask(taskResource));
            } catch (ServiceMandatoryFieldValidationException e) {
                log.error("Error when reconfiguring task({})",
                          MANDATORY_FIELD_MISSING_ERROR.getDetail() + taskId + e.getMessage(), e);
                throw e;
            }
        } else {
            optionalTaskResource = cftTaskDatabaseService.findByIdOnly(taskId);
            if (optionalTaskResource.isPresent()) {
                TaskResource taskResource = optionalTaskResource.get();
                log.info("did not execute reconfigure for Task Resource: taskId: {}, caseId: {}, state: {}",
                         taskResource.getTaskId(), taskResource.getCaseId(), taskResource.getState()
                );
            } else {
                log.info("Could not find task to reconfigure : taskId: {}", taskId);
            }
            return Optional.empty();
        }
    }

}
