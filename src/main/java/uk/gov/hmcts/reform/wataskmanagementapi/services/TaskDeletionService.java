package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getTaskIds;

@Slf4j
@Service
public class TaskDeletionService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    @Autowired
    public TaskDeletionService(final CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    public void deleteTasksByCaseId(final String caseId) {
        final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders = cftTaskDatabaseService
                .findByTaskIdsByCaseId(caseId);

        filterAllUnterminatedTasksAndLogError(taskResourceCaseQueryBuilders, caseId);

        deleteTasks(taskResourceCaseQueryBuilders, caseId);
    }

    private void deleteTasks(final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders,
                             final String caseId) {
        try {
            cftTaskDatabaseService.deleteTasks(getTaskIds(taskResourceCaseQueryBuilders));
        } catch (final Exception exception) {
            log.error(String.format("Unable to delete all tasks for case id: %s", caseId));
            log.error("Exception occurred: {}", exception.getMessage(), exception);
        }
    }

    private void filterAllUnterminatedTasksAndLogError(final List<TaskResourceCaseQueryBuilder>
                                                               taskResourceCaseQueryBuilders,
                                                       final String caseId) {
        final List<String> unterminatedTaskIds = taskResourceCaseQueryBuilders
                .stream()
                .filter(task -> !task.getState().equals(TERMINATED))
                .map(TaskResourceCaseQueryBuilder::getTaskId)
                .toList();

        if (!unterminatedTaskIds.isEmpty()) {
            log.error(String.format("Deleted some UNTERMINATED tasks: %s for caseId: %s", unterminatedTaskIds, caseId));
        }
    }
}