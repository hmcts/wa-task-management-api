package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators;

import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

public interface TaskConfigurator {
    TaskConfigurationResults getConfigurationVariables(TaskToConfigure taskToConfigure);
}
