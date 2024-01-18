package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Service
public class TaskOperationService {

    private final List<TaskOperationPerformService> taskOperationPerformServices;

    public TaskOperationService(List<TaskOperationPerformService> taskOperationPerformServices) {
        this.taskOperationPerformServices = taskOperationPerformServices;
    }

    @SuppressWarnings("unchecked")
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {

        return taskOperationPerformServices.stream()
            .map(taskOperationServices -> taskOperationServices.performOperation(taskOperationRequest))
            .filter(Objects::nonNull)
            .filter(response -> response.getResponseMap() != null)
            .findFirst()
            .orElseGet(() -> new TaskOperationResponse(new HashMap<>()));
    }
}
