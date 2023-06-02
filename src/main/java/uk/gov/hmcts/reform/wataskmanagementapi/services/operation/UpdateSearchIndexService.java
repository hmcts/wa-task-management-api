package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class UpdateSearchIndexService implements TaskOperationPerformService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    public UpdateSearchIndexService(CFTTaskDatabaseService cftTaskDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    @Override
    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType().equals(TaskOperationType.UPDATE_SEARCH_INDEX)) {
            return updateSearchIndex();
        }
        return List.of();
    }

    private List<TaskResource> updateSearchIndex() {
        List<TaskResource> taskToReIndexed =  cftTaskDatabaseService.findTaskToUpdateIndex();
        List<TaskResource> successfulTaskResources = new ArrayList<>();

        if (taskToReIndexed != null) {
            taskToReIndexed.forEach(t -> {
                log.info("Update search index for task-id {}", t.getTaskId());
                Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
                    .findByIdAndWaitAndObtainPessimisticWriteLock(t.getTaskId());

                if (optionalTaskResource.isPresent()) {
                    TaskResource taskResource = optionalTaskResource.get();
                    taskResource.setIndexed(true);
                    successfulTaskResources.add(cftTaskDatabaseService.saveTask(taskResource));
                }
            });
        }

        return successfulTaskResources;
    }
}
