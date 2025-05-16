package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskReconfigurationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.MARK_FOR_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.setTaskActionAttributes;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class MarkTaskReconfigurationService implements TaskOperationPerformService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final IdamTokenGenerator idamTokenGenerator;

    public MarkTaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                          IdamTokenGenerator idamTokenGenerator) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.idamTokenGenerator = idamTokenGenerator;
    }

    protected TaskOperationResponse markTasksToReconfigure(List<TaskFilter<?>> taskFilters) {
        List<String> caseIds = taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("case_id"))
            .flatMap(filter -> ((MarkTaskToReconfigureTaskFilter) filter).getValues().stream())
            .map(Object::toString)
            .toList();

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
                caseIds, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

        List<TaskResource> successfulTaskResources = new ArrayList<>();
        List<String> taskIds = taskResources.stream()
            .map(TaskResource::getTaskId)
            .toList();

        List<String> failedTaskIds = updateReconfigureRequestTime(taskIds, successfulTaskResources);

        if (!failedTaskIds.isEmpty()) {
            failedTaskIds = updateReconfigureRequestTime(failedTaskIds, successfulTaskResources);
        }

        if (!failedTaskIds.isEmpty()) {
            throw new TaskReconfigurationException(TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED, caseIds);
        }

        return new TaskOperationResponse(Map.of("successfulTaskResources", successfulTaskResources.size()));
    }

    @Override
    @Transactional(noRollbackFor = TaskReconfigurationException.class)
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getType().equals(TaskOperationType.MARK_TO_RECONFIGURE)) {
            return markTasksToReconfigure(taskOperationRequest.getTaskFilter());
        }
        return new TaskOperationResponse();
    }

    private List<String> updateReconfigureRequestTime(List<String> taskIds,
                                                      List<TaskResource> successfulTaskResources) {
        List<String> failedTaskIds = new ArrayList<>();
        taskIds.forEach(taskId -> {

            try {
                log.info("Mark task-id {} to reconfigure", taskId);
                Optional<TaskResource> optionalTaskResource = cftTaskDatabaseService
                    .findByIdAndStateInObtainPessimisticWriteLock(
                        taskId, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

                if (optionalTaskResource.isPresent()) {
                    TaskResource taskResource = optionalTaskResource.get();
                    taskResource.setReconfigureRequestTime(OffsetDateTime.now());
                    taskResource.setIndexed(false);
                    updateTaskActionAttributes(taskResource);
                    successfulTaskResources.add(cftTaskDatabaseService.saveTask(taskResource));
                }
            } catch (Exception e) {
                log.error("Error marking task-id {} to reconfigure", taskId, e);
                failedTaskIds.add(taskId);
            }
        });

        return failedTaskIds;
    }

    private void updateTaskActionAttributes(TaskResource taskResource) {
        String systemUserToken = idamTokenGenerator.generate();
        String systemUserId = idamTokenGenerator.getUserInfo(systemUserToken).getUid();
        setTaskActionAttributes(taskResource, systemUserId, MARK_FOR_RECONFIGURE);
    }


}
