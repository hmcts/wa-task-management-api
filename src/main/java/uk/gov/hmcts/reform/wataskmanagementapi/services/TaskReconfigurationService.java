package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class TaskReconfigurationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public TaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    public void markTasksToReconfigure(List<TaskFilter> taskFilters) {
        List<Object> caseIds = taskFilters.stream().filter(filter -> filter.getKey().equalsIgnoreCase("case_id"))
            .flatMap(filter ->filter.getValues().stream())
            .collect(Collectors.toList());

        List<TaskResource> taskResources = caseIds.stream()
            .map(caseId -> cftTaskDatabaseService.findByCaseIdOnly((String)caseId))
            .flatMap(tasks -> tasks.stream())
            .filter(task -> Objects.isNull(task.getReconfigureRequestTime()))
            .collect(Collectors.toList());

        taskResources.stream().forEach(taskResource -> {
            taskResource.setReconfigureRequestTime(OffsetDateTime.now());
            cftTaskDatabaseService.saveTask(taskResource);
        });
    }
}
