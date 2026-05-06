package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;

@Service
public class TaskInitiationPushService {

    private static final String UNCONFIGURED = "unconfigured";

    private final CamundaTaskInitiationRequestMapper requestMapper;
    private final TaskManagementService taskManagementService;

    public TaskInitiationPushService(CamundaTaskInitiationRequestMapper requestMapper,
                                     TaskManagementService taskManagementService) {
        this.requestMapper = requestMapper;
        this.taskManagementService = taskManagementService;
    }

    public TaskResource initiateTask(String taskId, CamundaTaskInitiationRequest request) {
        CamundaVariable cftTaskState = request.getVariables() == null
            ? null
            : request.getVariables().get(CFT_TASK_STATE.value());
        String cftTaskStateValue = cftTaskState == null || cftTaskState.getValue() == null
            ? null
            : cftTaskState.getValue().toString();
        if (!UNCONFIGURED.equals(cftTaskStateValue)) {
            throw new IllegalArgumentException("Task must be in unconfigured state for push initiation");
        }

        InitiateTaskRequestMap initiateTaskRequest = requestMapper.map(taskId, request);
        TaskResource savedTask = taskManagementService.initiateTask(taskId, initiateTaskRequest);
        taskManagementService.updateTaskIndex(savedTask.getTaskId());
        return savedTask;
    }
}
