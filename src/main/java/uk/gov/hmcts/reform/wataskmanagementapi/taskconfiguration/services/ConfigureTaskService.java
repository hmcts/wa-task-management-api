package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;

@Slf4j
@Component
public class ConfigureTaskService {

    private final TaskConfigurationCamundaService taskConfigurationCamundaService;
    private final List<TaskConfigurator> taskConfigurators;
    private final TaskAutoAssignmentService taskAutoAssignmentService;
    private final CaseConfigurationProviderService caseConfigurationProviderService;
    private final CFTTaskMapper cftTaskMapper;
    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;

    public ConfigureTaskService(TaskConfigurationCamundaService taskConfigurationCamundaService,
                                List<TaskConfigurator> taskConfigurators,
                                TaskAutoAssignmentService taskAutoAssignmentService,
                                CaseConfigurationProviderService caseConfigurationProviderService,
                                CFTTaskMapper cftTaskMapper,
                                LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.taskConfigurationCamundaService = taskConfigurationCamundaService;
        this.taskConfigurators = taskConfigurators;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
        this.caseConfigurationProviderService = caseConfigurationProviderService;
        this.cftTaskMapper = cftTaskMapper;
        this.featureFlagProvider = featureFlagProvider;
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter"})
    public void configureTask(String taskId) {
        CamundaTask task = taskConfigurationCamundaService.getTask(taskId);
        log.info("CamundaTask id '{}' retrieved from Camunda", task.getId());

        Map<String, CamundaValue<Object>> processVariables = taskConfigurationCamundaService.getVariables(taskId);
        CamundaValue<Object> caseIdValue = processVariables.get(CamundaVariableDefinition.CASE_ID.value());
        CamundaValue<Object> taskTypeIdValue = processVariables.get(CamundaVariableDefinition.TASK_ID.value());
        String caseId = (String) caseIdValue.getValue();
        String taskTypeId = (String) taskTypeIdValue.getValue();

        TaskToConfigure taskToConfigure
            = new TaskToConfigure(taskId, taskTypeId, caseId, task.getName(), null);

        TaskConfigurationResults configurationResults = getConfigurationResults(taskToConfigure);

        Map<String, CamundaValue<String>> processVariablesToAdd =
            convertToCamundaFormat(configurationResults.getProcessVariables());

        //Update Variables
        taskConfigurationCamundaService.addProcessVariables(taskId, processVariablesToAdd);

        //Get latest task state as it was updated previously
        CamundaValue<String> taskState = processVariablesToAdd.get(TASK_STATE.value());

        //Attempt to auto assign task if possible
        taskAutoAssignmentService.autoAssignTask(taskToConfigure, taskState.getValue());

    }

    public ConfigureTaskResponse getConfiguration(TaskToConfigure taskToConfigure) {

        TaskConfigurationResults configurationResults = getConfigurationResults(taskToConfigure);

        AutoAssignmentResult autoAssignmentResult = taskAutoAssignmentService
            .getAutoAssignmentVariables(taskToConfigure);

        configurationResults.getProcessVariables().put(TASK_STATE.value(), autoAssignmentResult.getTaskState());

        if (autoAssignmentResult.getAssignee() != null) {
            configurationResults.getProcessVariables().put(AUTO_ASSIGNED.value(), true);
        }

        return new ConfigureTaskResponse(
            taskToConfigure.getId(),
            taskToConfigure.getCaseId(),
            autoAssignmentResult.getAssignee(),
            configurationResults.getProcessVariables()
        );
    }

    public TaskResource configureCFTTask(TaskResource skeletonMappedTask, TaskToConfigure taskToConfigure) {
        TaskToConfigure.TaskToConfigureBuilder taskToConfigureBuilder = taskToConfigure.toBuilder();
        if (featureFlagProvider.getBooleanValue(
            RELEASE_2_ENDPOINTS_FEATURE,
            StringUtils.EMPTY,
            StringUtils.EMPTY
        )) {
            Map<String, Object> taskAttributes = cftTaskMapper.getTaskAttributes(skeletonMappedTask);
            taskToConfigureBuilder.taskAttributes(taskAttributes);
        }

        TaskConfigurationResults configurationVariables = getConfigurationResults(taskToConfigureBuilder.build());
        return cftTaskMapper.mapConfigurationAttributes(skeletonMappedTask, configurationVariables);
    }

    public TaskResource reconfigureCFTTask(TaskResource taskResource) {
        Map<String, Object> taskAttributes = cftTaskMapper.getTaskAttributes(taskResource);

        TaskConfigurationResults configurationVariables = caseConfigurationProviderService
            .getCaseRelatedConfiguration(taskResource.getCaseId(), taskAttributes);

        return cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, configurationVariables);
    }

    private Map<String, CamundaValue<String>> convertToCamundaFormat(Map<String, Object> configurationVariables) {
        return configurationVariables.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                mappedDetail -> stringValue(mappedDetail.getValue().toString())
            ));
    }

    private TaskConfigurationResults getConfigurationResults(TaskToConfigure task) {
        TaskConfigurationResults configurationResults = new TaskConfigurationResults(new ConcurrentHashMap<>());

        //loop through all task configurators in order and add results to configurationResults
        taskConfigurators.stream()
            .map(configurator -> configurator.getConfigurationVariables(task))
            .forEach(result -> combineResults(result, configurationResults));

        return configurationResults;
    }

    /**
     * Helper method to combine a task configuration result into one final object
     * containing all process variables and evaluations.
     *
     * @param result               the task configuration result returned by the
     *                             configuratorCaseRelatedVariablesConfiguratorTest
     * @param configurationResults the final object where values get added
     */
    private void combineResults(TaskConfigurationResults result,
                                TaskConfigurationResults configurationResults) {

        if (result.getProcessVariables() != null && configurationResults.getProcessVariables() != null) {
            configurationResults.getProcessVariables().putAll(result.getProcessVariables());
        }

        if (result.getConfigurationDmnResponse() != null) {
            configurationResults.setConfigurationDmnResponse(result.getConfigurationDmnResponse());
        }

        if (result.getPermissionsDmnResponse() != null) {
            configurationResults.setPermissionsDmnResponse(result.getPermissionsDmnResponse());
        }
    }
}
