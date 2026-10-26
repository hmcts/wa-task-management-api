package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.DecisionTable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.configurators.TaskConfigurator;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConfigureTaskService {

    private static final List<String> SAFE_RECONFIGURATION_INPUT_FIELDS = List.of(
        "taskType",
        "taskState",
        "taskSystem",
        "securityClassification",
        "autoAssigned",
        "workType",
        "workTypeResource",
        "roleCategory",
        "hasWarnings",
        "assignmentExpiry",
        "businessContext",
        "executionTypeCode",
        "terminationProcess",
        "reconfigureRequestTime",
        "lastReconfigurationTime",
        "nextHearingDate",
        "priorityDate",
        "lastUpdatedTimestamp",
        "indexed"
    );

    private final List<TaskConfigurator> taskConfigurators;
    private final CaseConfigurationProviderService caseConfigurationProviderService;
    private final CFTTaskMapper cftTaskMapper;

    @Value("${task-diagnostics.log-reconfiguration-dmn-details:false}")
    private boolean logReconfigurationDmnDetails;

    public ConfigureTaskService(List<TaskConfigurator> taskConfigurators,
                                CaseConfigurationProviderService caseConfigurationProviderService,
                                CFTTaskMapper cftTaskMapper) {
        this.taskConfigurators = taskConfigurators;
        this.caseConfigurationProviderService = caseConfigurationProviderService;
        this.cftTaskMapper = cftTaskMapper;
    }

    public TaskResource configureCFTTask(TaskResource skeletonMappedTask, TaskToConfigure taskToConfigure) {
        TaskConfigurationResults configurationVariables = getConfigurationResults(taskToConfigure);
        return cftTaskMapper.mapConfigurationAttributes(skeletonMappedTask, configurationVariables);
    }

    public TaskResource reconfigureCFTTask(TaskResource taskResource) {
        Map<String, Object> taskAttributes = cftTaskMapper.getTaskAttributes(taskResource);

        if (logReconfigurationDmnDetails) {
            log.info(
                "Reconfiguration DMN input: taskId={}, decisionTableKey={}, inputKeys={}, safeInputs={}, "
                    + "storedNextHearingDateBefore={}",
                taskResource.getTaskId(),
                configurationDecisionTableKey(taskResource),
                taskAttributes.keySet(),
                safeInputSummary(taskAttributes),
                taskResource.getNextHearingDate()
            );
        }

        TaskConfigurationResults configurationVariables = caseConfigurationProviderService
            .getCaseRelatedConfiguration(taskResource.getCaseId(), taskAttributes, true);

        if (logReconfigurationDmnDetails) {
            logNextHearingDateDmnResult(taskResource.getTaskId(), configurationVariables);
        }

        TaskResource reconfiguredTask =
            cftTaskMapper.reconfigureTaskResourceFromDmnResults(taskResource, configurationVariables);

        if (logReconfigurationDmnDetails) {
            log.info(
                "Reconfiguration DMN applied: taskId={}, storedNextHearingDateAfter={}",
                taskResource.getTaskId(),
                reconfiguredTask.getNextHearingDate()
            );
        }

        return reconfiguredTask;
    }

    private static String configurationDecisionTableKey(TaskResource taskResource) {
        if (taskResource.getJurisdiction() == null || taskResource.getCaseTypeId() == null) {
            return "unavailable";
        }
        return DecisionTable.WA_TASK_CONFIGURATION.getTableKey(
            taskResource.getJurisdiction(), taskResource.getCaseTypeId());
    }

    private static Map<String, Object> safeInputSummary(Map<String, Object> taskAttributes) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (String field : SAFE_RECONFIGURATION_INPUT_FIELDS) {
            String actualField = actualFieldName(taskAttributes, field);
            if (actualField != null) {
                summary.put(actualField, safeInputValue(field, taskAttributes.get(actualField)));
            }
        }
        for (String field : List.of("notes", "taskRoleResources")) {
            String actualField = actualFieldName(taskAttributes, field);
            if (actualField != null) {
                Object value = taskAttributes.get(actualField);
                summary.put(field, value instanceof Collection<?> collection
                    ? "count=" + collection.size()
                    : value == null ? "null" : "present");
            }
        }
        for (String field : List.of("terminationReason", "lastUpdatedUser", "lastUpdatedAction")) {
            String actualField = actualFieldName(taskAttributes, field);
            if (actualField != null) {
                summary.put(actualField,
                    taskAttributes.get(actualField) == null ? "null" : "present; value redacted");
            }
        }
        return summary;
    }

    private static String actualFieldName(Map<String, Object> taskAttributes, String field) {
        if (taskAttributes.containsKey(field)) {
            return field;
        }
        String snakeCaseField = field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        return taskAttributes.containsKey(snakeCaseField) ? snakeCaseField : null;
    }

    private static Object safeInputValue(String field, Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
            || value instanceof Enum<?> || value instanceof TemporalAccessor) {
            return value;
        }
        if (value instanceof WorkTypeResource workTypeResource) {
            return workTypeResource.getId();
        }
        if (value instanceof ExecutionTypeResource executionTypeResource) {
            return executionTypeResource.getExecutionCode();
        }
        return "present; value redacted (" + field + ")";
    }

    private static void logNextHearingDateDmnResult(String taskId,
                                                    TaskConfigurationResults configurationVariables) {
        List<ConfigurationDmnEvaluationResponse> responses = configurationVariables.getConfigurationDmnResponse();
        if (responses == null) {
            log.info("Reconfiguration DMN nextHearingDate result: taskId={}, response=null", taskId);
            return;
        }

        List<ConfigurationDmnEvaluationResponse> nextHearingDateResponses = responses.stream()
            .filter(response -> response.getName() != null
                && "nextHearingDate".equals(response.getName().getValue()))
            .toList();

        if (nextHearingDateResponses.isEmpty()) {
            log.info("Reconfiguration DMN nextHearingDate result: taskId={}, row=absent", taskId);
            return;
        }

        nextHearingDateResponses.forEach(response -> log.info(
            "Reconfiguration DMN nextHearingDate result: taskId={}, value={}, canReconfigure={}",
            taskId,
            response.getValue() == null ? null : response.getValue().getValue(),
            response.getCanReconfigure() == null ? null : response.getCanReconfigure().getValue()
        ));
    }

    private TaskConfigurationResults getConfigurationResults(TaskToConfigure task) {
        TaskConfigurationResults configurationResults = new TaskConfigurationResults(new ConcurrentHashMap<>());

        //loop through all task configurators in order and add results to configurationResults
        taskConfigurators.stream()
            .map(configurator -> configurator.getConfigurationVariables(task))
            .forEach(result -> combineResults(result, configurationResults));

        return configurationResults;
    }

    /**
     * Helper method to combine a task configuration result into one final object
     * containing all process variables and evaluations.
     *
     * @param result               the task configuration result returned by the
     *                             configuratorCaseRelatedVariablesConfiguratorTest
     * @param configurationResults the final object where values get added
     */
    private void combineResults(TaskConfigurationResults result,
                                TaskConfigurationResults configurationResults) {

        if (result.getProcessVariables() != null) {
            configurationResults.getProcessVariables().putAll(result.getProcessVariables());
        }

        if (result.getConfigurationDmnResponse() != null) {
            configurationResults.setConfigurationDmnResponse(result.getConfigurationDmnResponse());
        }

        if (result.getPermissionsDmnResponse() != null) {
            configurationResults.setPermissionsDmnResponse(result.getPermissionsDmnResponse());
        }
    }
}
