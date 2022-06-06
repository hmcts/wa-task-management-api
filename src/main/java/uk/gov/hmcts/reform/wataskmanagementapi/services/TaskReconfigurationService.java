package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskReconfigurationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;

    public TaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                      ConfigureTaskService configureTaskService,
                                      TaskAutoAssignmentService taskAutoAssignmentService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
    }

    @Transactional(noRollbackFor = TaskReconfigurationException.class)
    public List<TaskResource> markTasksToReconfigure(List<TaskFilter<?>> taskFilters) {
        List<String> caseIds = taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("case_id"))
            .flatMap(filter -> ((MarkTaskToReconfigureTaskFilter) filter).getValues().stream())
            .map(Object::toString)
            .collect(Collectors.toList());

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
                caseIds, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        List<TaskResource> successfulTaskResources = new ArrayList<>();
        List<String> taskIds = taskResources.stream()
            .map(TaskResource::getTaskId)
            .collect(Collectors.toList());

        List<String> failedTaskIds = updateReconfigureRequestTime(taskIds, successfulTaskResources);

        if (!failedTaskIds.isEmpty()) {
            failedTaskIds = updateReconfigureRequestTime(failedTaskIds, successfulTaskResources);
        }

        if (!failedTaskIds.isEmpty()) {
            throw new TaskReconfigurationException(TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED, caseIds);
        }

        return successfulTaskResources;
    }


    private List<String> updateReconfigureRequestTime(List<String> taskIds,
                                                      List<TaskResource> successfulTaskResources) {
        List<String> failedTaskIds = new ArrayList<>();
        taskIds.forEach(taskId -> {

            try {
                Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
                    .findByIdAndObtainPessimisticWriteLock(taskId);

                if (optionalTaskResource.isPresent()) {
                    TaskResource taskResource = optionalTaskResource.get();
                    taskResource.setReconfigureRequestTime(OffsetDateTime.now());
                    successfulTaskResources.add(cftTaskDatabaseService.saveTask(taskResource));
                }
            } catch (Exception e) {
                failedTaskIds.add(taskId);
            }
        });

        return failedTaskIds;
    }

    @Transactional(noRollbackFor = TaskReconfigurationException.class)
    public List<TaskResource> executeReconfigure(List<TaskFilter<?>> taskFilters) {

        OffsetDateTime reconfigureDateTime = getReconfigureRequestTime(taskFilters);

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
