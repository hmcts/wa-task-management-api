package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.EXECUTE_RECONFIGURE;

@Service
public class TaskOperationService {

    private final TaskManagementService taskManagementService;
    private final List<TaskOperationPerformService> taskOperationPerformServices;

    public TaskOperationService(TaskManagementService taskManagementService,
                                List<TaskOperationPerformService> taskOperationPerformServices) {
        this.taskManagementService = taskManagementService;
        this.taskOperationPerformServices = taskOperationPerformServices;
    }

    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        List<TaskResource> successfulTaskResources = taskOperationPerformServices.stream()
            .flatMap(taskOperationService -> taskOperationService
                .performOperation(taskOperationRequest).stream())
            .filter(Objects::nonNull)
            .toList();

        if (EXECUTE_RECONFIGURE.equals(taskOperationRequest.getOperation().getType())
            && successfulTaskResources != null) {
            successfulTaskResources.forEach(t -> taskManagementService.updateTaskIndex(t.getTaskId()));
        }

        return successfulTaskResources;
    }
}
