package uk.gov.hmcts.reform.wataskmanagementapi.services.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;

import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState.CONFIGURED;

@Component
@Order(1)
public class ConstantVariablesConfigurator implements TaskConfigurator {

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public TaskConfigurationResults getConfigurationVariables(TaskToConfigure task) {

        Map<String, Object> processVariables = Map.of(
            TASK_STATE.value(), CONFIGURED.value(),
            AUTO_ASSIGNED.value(), false,
            EXECUTION_TYPE.value(), "Case Management Task",
            TASK_SYSTEM.value(), "SELF"
        );

        return new TaskConfigurationResults(processVariables);
    }
}
