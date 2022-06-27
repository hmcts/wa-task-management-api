package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ExecuteTaskReconfigurationService implements TaskOperationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;

    public ExecuteTaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                             ConfigureTaskService configureTaskService,
                                             TaskAutoAssignmentService taskAutoAssignmentService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
    }

    @Override
    @Transactional(noRollbackFor = TaskReconfigurationException.class)
    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getName().equals(TaskOperationName.EXECUTE_RECONFIGURE)) {
            return executeTasksToReconfigure(taskOperationRequest);
        }
        return List.of();
    }

    private List<TaskResource> executeTasksToReconfigure(TaskOperationRequest request) {
        OffsetDateTime reconfigureDateTime = getReconfigureRequestTime(request.getTaskFilter());

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsNotNull(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        taskResources.stream()
            .map(task -> cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(task.getTaskId())
                .orElseGet(() -> null))
            .filter(Objects::nonNull)
            .filter(task -> task.getReconfigureRequestTime().isAfter(reconfigureDateTime))
            .forEach(taskResource -> {
                taskResource = configureTask(taskResource);
                taskResource = taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);
                taskResource.setReconfigureRequestTime(null);
                taskResource.setLastReconfigurationTime(OffsetDateTime.now());
                cftTaskDatabaseService.saveTask(taskResource);
            });

        return taskResources;
    }

    private OffsetDateTime getReconfigureRequestTime(List<TaskFilter<?>> taskFilters) {

        return (OffsetDateTime) taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("reconfigure_request_time"))
            .findFirst().get().getValues();
    }

    private TaskResource configureTask(TaskResource taskResource) {
        TaskToConfigure taskToConfigure = new TaskToConfigure(
            taskResource.getTaskId(),
            taskResource.getTaskType(),
            taskResource.getCaseId(),
            taskResource.getTaskName()
        );

        return configureTaskService.configureCFTTask(
            taskResource,
            taskToConfigure
        );
    }

}