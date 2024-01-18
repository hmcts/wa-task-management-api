package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;

import java.util.Objects;

@Component
@Order(3)
public class CaseRelatedVariablesConfigurator implements TaskConfigurator {

    private final CaseConfigurationProviderService caseConfigurationProviderService;

    public CaseRelatedVariablesConfigurator(CaseConfigurationProviderService caseConfigurationProviderService) {
        this.caseConfigurationProviderService = caseConfigurationProviderService;
    }

    @Override
    public TaskConfigurationResults getConfigurationVariables(TaskToConfigure task) {

        Objects.requireNonNull(task.getCaseId(), String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No caseId process variable.",
            task.getId()
        ));

        return caseConfigurationProviderService
            .getCaseRelatedConfiguration(task.getCaseId(), task.getTaskAttributes(), false);
    }
}
