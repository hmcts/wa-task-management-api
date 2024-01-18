package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;

public interface TaskConfigurator {
    TaskConfigurationResults getConfigurationVariables(TaskToConfigure taskToConfigure);
}
