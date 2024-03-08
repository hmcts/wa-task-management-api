package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.configurators.TaskConfigurator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConfigureTaskService {

    private final List<TaskConfigurator> taskConfigurators;
    private final CaseConfigurationProviderService caseConfigurationProviderService;
    private final CFTTaskMapper cftTaskMapper;

    public ConfigureTaskService(List<TaskConfigurator> taskConfigurators,
                                CaseConfigurationProviderService caseConfigurationProviderService,
                                CFTTaskMapper cftTaskMapper) {
        this.taskConfigurators = taskConfigurators;
        this.caseConfigurationProviderService = caseConfigurationProviderService;
        this.cftTaskMapper = cftTaskMapper;
    }

    public TaskResource configureCFTTask(TaskResource skeletonMappedTask, TaskToConfigure taskToConfigure) {
        TaskConfigurationResults configurationVariables = getConfigurationResults(taskToConfigure);
        return cftTaskMapper.mapConfigurationAttributes(skeletonMappedTask, configurationVariables);
    }

    public TaskResource reconfigureCFTTask(TaskResource taskResource) {
        Map<String, Object> taskAttributes = cftTaskMapper.getTaskAttributes(taskResource);

        TaskConfigurationResults configurationVariables = caseConfigurationProviderService
            .getCaseRelatedConfiguration(taskResource.getCaseId(), taskAttributes, true);

        return cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, configurationVariables);
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

        if (result.getProcessVariables() != null) {
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
