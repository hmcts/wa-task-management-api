package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Transactional
    public List<TaskResource> markTasksToReconfigure(List<TaskFilter<?>> taskFilters) {
        List<String> caseIds = taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("case_id"))
            .flatMap(filter -> ((MarkTaskToReconfigureTaskFilter)filter).getValues().stream())
            .map(Object::toString)
            .collect(Collectors.toList());

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
                caseIds, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        taskResources.stream()
            .map(task -> cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(task.getTaskId())
                .orElseGet(() -> null))
            .filter(Objects::nonNull)
            .forEach(taskResource -> {
                taskResource.setReconfigureRequestTime(OffsetDateTime.now());
                cftTaskDatabaseService.saveTask(taskResource);
            });

        return taskResources;
    }

    @Transactional
    public List<TaskResource> executeReconfigure(List<TaskFilter<?>> taskFilters) {

        OffsetDateTime reconfigureDate = OffsetDateTime.parse(getReconfigureRequestTime(taskFilters));

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsNotNull(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        taskResources.stream()
            .map(task -> cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(task.getTaskId())
                .orElseGet(() -> null))
            .filter(Objects::nonNull)
            .filter(task -> task.getReconfigureRequestTime().isAfter(reconfigureDate))
            .forEach(taskResource -> {
                configureTaskService.configureTask(taskResource.getTaskId());
                taskAutoAssignmentService.reAutoAssignCFTTask(taskResource);
                taskResource.setReconfigureRequestTime(null);
                taskResource.setLastReconfigurationTime(OffsetDateTime.now());
                cftTaskDatabaseService.saveTask(taskResource);
            });

        return taskResources;
    }

    @Transactional
    public String getReconfigureRequestTime(List<TaskFilter<?>> taskFilters) {

        return taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("reconfigure_request_time"))
            .findFirst().get().getValues().toString();
    }
}
