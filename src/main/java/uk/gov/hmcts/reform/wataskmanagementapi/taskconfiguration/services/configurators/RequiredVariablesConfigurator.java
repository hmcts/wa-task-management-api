package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
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
        return getProcessVariables(task.getCaseId(), task.getId(), task.getTaskTypeId(), task.getName());
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public TaskConfigurationResults getConfigurationVariables(TaskResource taskResource) {
        return getProcessVariables(
            taskResource.getCaseId(),
            taskResource.getTaskId(),
            taskResource.getTaskType(),
            taskResource.getTaskName()
        );
    }

    private TaskConfigurationResults getProcessVariables(String caseId,
                                                         String taskId,
                                                         String taskTypeId,
                                                         String taskName) {
        requireNonNull(caseId, String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'caseId' process variable.",
            taskId
        ));

        requireNonNull(taskTypeId, String.format(
            "Task with id '%s' cannot be configured it has not been setup correctly. No 'taskTypeId' process variable.",
            taskId
        ));

        Map<String, Object> processVariables = Map.of(
            CASE_ID.value(), caseId,
            TITLE.value(), taskName,
            TASK_TYPE.value(), taskTypeId
        );

        return new TaskConfigurationResults(processVariables);
    }
}
