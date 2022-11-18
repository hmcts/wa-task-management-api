package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskToConfigure;

public interface TaskConfigurator {
    TaskConfigurationResults getConfigurationVariables(TaskToConfigure taskToConfigure);
}
