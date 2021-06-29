package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response.ConfigureTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.configurators.TaskConfigurator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums.CamundaVariableDefinition.AUTO_ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums.CamundaVariableDefinition.TASK_STATE;

@Slf4j
@Component
public class ConfigureTaskService {

    private final TaskConfigurationCamundaService taskConfigurationCamundaService;
    private final List<TaskConfigurator> taskConfigurators;
    private final TaskAutoAssignmentService taskAutoAssignmentService;

    public ConfigureTaskService(TaskConfigurationCamundaService taskConfigurationCamundaService,
                                List<TaskConfigurator> taskConfigurators,
                                TaskAutoAssignmentService taskAutoAssignmentService) {
        this.taskConfigurationCamundaService = taskConfigurationCamundaService;
        this.taskConfigurators = taskConfigurators;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter"})
    public void configureTask(String taskId) {

        CamundaTask task = taskConfigurationCamundaService.getTask(taskId);
        log.info("CamundaTask id '{}' retrieved from Camunda", task.getId());

        Map<String, CamundaValue<Object>> processVariables = taskConfigurationCamundaService.getVariables(taskId);
        CamundaValue<Object> caseIdValue = processVariables.get(CamundaVariableDefinition.CASE_ID.value());
        String caseId = (String) caseIdValue.getValue();

        HashMap<String, Object> variables = new HashMap<>();
        processVariables.forEach((key, value) -> variables.put(key, value.getValue()));

        TaskToConfigure taskToConfigure = new TaskToConfigure(
            taskId,
            caseId,
            task.getName(),
            variables
        );

        Map<String, Object> configurationVariables = getConfigurationVariables(taskToConfigure);

        Map<String, CamundaValue<String>> processVariablesToAdd = configurationVariables.entrySet().stream()
            .collect(toMap(
                Map.Entry::getKey,
                mappedDetail -> stringValue(mappedDetail.getValue().toString())
            ));

        //Update Variables
        taskConfigurationCamundaService.addProcessVariables(taskId, processVariablesToAdd);

        //Get latest task state as it was updated previously
        CamundaValue<String> taskState = processVariablesToAdd.get(TASK_STATE.value());

        //Attempt to auto assign task if possible
        taskAutoAssignmentService.autoAssignTask(taskToConfigure, taskState.getValue());

    }

    public ConfigureTaskResponse getConfiguration(TaskToConfigure taskToConfigure) {

        Map<String, Object> configurationVariables = getConfigurationVariables(taskToConfigure);

        AutoAssignmentResult autoAssignmentResult = taskAutoAssignmentService
            .getAutoAssignmentVariables(taskToConfigure);

        configurationVariables.put(TASK_STATE.value(), autoAssignmentResult.getTaskState());

        if (autoAssignmentResult.getAssignee() != null) {
            configurationVariables.put(AUTO_ASSIGNED.value(), true);
        }

        return new ConfigureTaskResponse(
            taskToConfigure.getId(),
            taskToConfigure.getCaseId(),
            autoAssignmentResult.getAssignee(),
            configurationVariables
        );
    }

    private Map<String, Object> getConfigurationVariables(TaskToConfigure task) {
        HashMap<String, Object> configurationVariables = new HashMap<>();

        //loop through all task configurators in order
        taskConfigurators.stream()
            .map(configurator -> configurator.getConfigurationVariables(task))
            .forEach(configurationVariables::putAll);

        return configurationVariables;
    }

}
