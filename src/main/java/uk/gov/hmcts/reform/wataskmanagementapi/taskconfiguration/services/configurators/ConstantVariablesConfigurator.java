package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.EXECUTION_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_SYSTEM;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.CONFIGURED;

@Component
@Order(1)
public class ConstantVariablesConfigurator implements TaskConfigurator {

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public Map<String, Object> getConfigurationVariables(TaskToConfigure task) {

        return Map.of(
            TASK_STATE.value(), CONFIGURED.value(),
            CamundaVariableDefinition.AUTO_ASSIGNED.value(), false,
            EXECUTION_TYPE.value(), "Case Management Task",
            TASK_SYSTEM.value(), "SELF"
        );
    }
}
