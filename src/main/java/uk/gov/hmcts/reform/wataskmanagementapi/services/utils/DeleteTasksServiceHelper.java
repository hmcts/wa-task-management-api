package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;

import java.util.List;


public final class DeleteTasksServiceHelper {

    private DeleteTasksServiceHelper() {
    }

    public static List<String> getTaskIds(final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders) {
        return taskResourceCaseQueryBuilders.stream()
                .map(TaskResourceCaseQueryBuilder::getTaskId)
                .toList();
    }
}
