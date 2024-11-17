package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskReconfigurationHelper {

    private CFTTaskDatabaseService cftTaskDatabaseService;
    private  ConfigureTaskService configureTaskService;
    private  TaskAutoAssignmentService taskAutoAssignmentService;

    public TaskReconfigurationHelper(@Autowired CFTTaskDatabaseService cftTaskDatabaseService,
                                     @Autowired ConfigureTaskService configureTaskService,
                                     @Autowired TaskAutoAssignmentService taskAutoAssignmentService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
    }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TaskResource reconfigureTaskResource(String taskId) {
        Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
            .findByIdAndStateInObtainPessimisticWriteLock(taskId, List.of(
                CFTTaskState.ASSIGNED,
                CFTTaskState.UNASSIGNED
            ));
        if (optionalTaskResource.isPresent()) {
            TaskResource taskResource = optionalTaskResource.get();
            taskResource = configureTaskService.reconfigureCFTTask(taskResource);
            taskResource = taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);
            taskResource.setReconfigureRequestTime(null);
            taskResource.setLastReconfigurationTime(OffsetDateTime.now());
            resetIndexed(taskResource);
            return cftTaskDatabaseService.saveTask(taskResource);
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
            return null;
        }
    }

}
