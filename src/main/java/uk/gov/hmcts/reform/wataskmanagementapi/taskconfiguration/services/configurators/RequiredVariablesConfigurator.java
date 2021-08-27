package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;

@Component
@Order(2)
public class RequiredVariablesConfigurator implements TaskConfigurator {

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public TaskConfigurationResults getConfigurationVariables(TaskToConfigure task) {

        requireNonNull(task.getCaseId(), String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'caseId' process variable.",
            task.getId()
        ));

        requireNonNull(task.getTaskTypeId(), String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'taskId' process variable.",
            task.getId()
        ));

        Map<String, Object> processVariables = Map.of(
            CASE_ID.value(), task.getCaseId(),
            TITLE.value(), task.getName(),
            TASK_TYPE.value(), task.getTaskTypeId()
        );

        return new TaskConfigurationResults(processVariables);
    }
}
