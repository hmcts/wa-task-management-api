package uk.gov.hmcts.reform.wataskmanagementapi.services.taskconfiguration.configurators;

import uk.gov.hmcts.reform.wataskconfigurationapi.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.taskconfiguration.TaskToConfigure;

import java.util.Map;

public interface TaskConfigurator {
    Map<String, Object> getConfigurationVariables(TaskToConfigure taskToConfigure);
}
