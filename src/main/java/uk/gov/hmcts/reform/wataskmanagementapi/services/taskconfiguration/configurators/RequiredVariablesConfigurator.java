package uk.gov.hmcts.reform.wataskmanagementapi.services.taskconfiguration.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.taskconfiguration.TaskToConfigure;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;

@Component
@Order(2)
public class RequiredVariablesConfigurator implements TaskConfigurator {

    @Override
    public Map<String, Object> getConfigurationVariables(TaskToConfigure task) {

        requireNonNull(task.getCaseId(), String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'caseId' process variable.",
            task.getId()
        ));

        requireNonNull(task.getProcessVariables().get("taskId"), String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'taskId' process variable.",
            task.getId()
        ));

        return Map.of(
            CASE_ID.value(), task.getCaseId(),
            TITLE.value(), task.getName(),
            TASK_TYPE.value(), task.getProcessVariables().get("taskId")
        );
    }
}
