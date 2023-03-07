package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskReconfigurationException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.MARK_FOR_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_RECONFIGURATION_MARK_TASKS_TO_RECONFIGURE_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.setTaskActionAttributes;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class MarkTaskReconfigurationService implements TaskOperationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CaseConfigurationProviderService caseConfigurationProviderService;
    private final IdamTokenGenerator idamTokenGenerator;

    public MarkTaskReconfigurationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                          CaseConfigurationProviderService caseConfigurationProviderService,
                                          IdamTokenGenerator idamTokenGenerator) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.caseConfigurationProviderService = caseConfigurationProviderService;
        this.idamTokenGenerator = idamTokenGenerator;
    }

    protected List<TaskResource> markTasksToReconfigure(List<TaskFilter<?>> taskFilters) {
        List<String> caseIds = taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("case_id"))
            .flatMap(filter -> ((MarkTaskToReconfigureTaskFilter) filter).getValues().stream())
            .map(Object::toString)
            .collect(Collectors.toList());

        List<String> reconfigurableCaseIds = caseIds.stream()
            .filter(this::isReconfigurable)
            .collect(Collectors.toList());

        List<TaskResource> taskResources = cftTaskDatabaseService
            .getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
                reconfigurableCaseIds, List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED));

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

    @Override
    @Transactional(noRollbackFor = TaskReconfigurationException.class)
    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        if (taskOperationRequest.getOperation().getName().equals(TaskOperationName.MARK_TO_RECONFIGURE)) {
            return markTasksToReconfigure(taskOperationRequest.getTaskFilter());
        }
        return List.of();
    }

    private boolean isReconfigurable(String caseId) {
        List<ConfigurationDmnEvaluationResponse> results = caseConfigurationProviderService
            .evaluateConfigurationDmn(caseId, null);
        return results.stream().filter(result -> result.getCanReconfigure() != null)
            .findAny()
            .map(result -> result.getCanReconfigure().getValue())
            .orElseGet(() -> false);
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
                    updateTaskActionAttributes(taskResource);
                    successfulTaskResources.add(cftTaskDatabaseService.saveTask(taskResource));
                }
            } catch (Exception e) {
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
