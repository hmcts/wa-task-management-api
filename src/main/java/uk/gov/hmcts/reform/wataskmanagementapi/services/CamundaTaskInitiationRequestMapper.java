package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CamundaTaskInitiationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CREATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DESCRIPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.PRIORITY_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.TASK_TYPE;

@Slf4j
@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CamundaTaskInitiationRequestMapper {

    private final ObjectMapper objectMapper;

    public CamundaTaskInitiationRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public InitiateTaskRequestMap map(String taskId, CamundaTaskInitiationRequest request) {
        Map<String, CamundaVariable> variables = request.getVariables() == null
            ? Map.of()
            : request.getVariables();

        String type = getType(taskId, variables);
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        attributes.put(TASK_TYPE.value(), type);
        attributes.put(DUE_DATE.value(), CAMUNDA_DATA_TIME_FORMATTER.format(request.getDue()));
        attributes.put(CREATED.value(), CAMUNDA_DATA_TIME_FORMATTER.format(request.getCreated()));
        attributes.put(TASK_NAME.value(), request.getName());

        if (request.getAssignee() != null) {
            attributes.put(ASSIGNEE.value(), request.getAssignee());
        }
        if (request.getDescription() != null) {
            attributes.put(DESCRIPTION.value(), request.getDescription());
        }

        variables.entrySet().stream()
            .filter(variable -> !variable.getKey().equals(DUE_DATE.value()))
            .filter(variable -> !variable.getKey().equals(ASSIGNEE.value()))
            .filter(variable -> !variable.getKey().equals(PRIORITY_DATE.value()))
            .filter(variable -> !variable.getKey().equals(DESCRIPTION.value()))
            .filter(variable -> !variable.getKey().equals(TASK_NAME.value()))
            .forEach(entry -> attributes.put(entry.getKey(), getCamundaVariableValue(entry.getValue())));

        return new InitiateTaskRequestMap(INITIATION, attributes);
    }

    private String getType(String taskId, Map<String, CamundaVariable> variables) {
        String type = getVariableValue(variables.get(TASK_TYPE.value()), String.class, null);
        if (type != null) {
            return type;
        }

        String fallbackTaskId = getVariableValue(variables.get(TASK_ID.value()), String.class, null);
        log.info(
            "Task '{}' did not have a 'taskType' defaulting to 'taskId' with value '{}'",
            taskId,
            fallbackTaskId
        );
        return fallbackTaskId;
    }

    private Object getCamundaVariableValue(CamundaVariable variable) {
        if (variable == null || variable.getType() == null) {
            return variable == null ? null : variable.getValue();
        }
        return switch (variable.getType()) {
            case "String" -> getVariableValue(variable, String.class, null);
            case "Boolean" -> getVariableValue(variable, Boolean.class, null);
            case "Integer" -> getVariableValue(variable, Integer.class, null);
            default -> variable.getValue();
        };
    }

    private <T> T getVariableValue(CamundaVariable variable, Class<T> type, T defaultValue) {
        Optional<T> value = map(variable, type);
        return value.orElse(defaultValue);
    }

    private <T> Optional<T> map(CamundaVariable variable, Class<T> type) {
        if (variable == null) {
            return Optional.empty();
        }
        T value = objectMapper.convertValue(variable.getValue(), type);
        return Optional.ofNullable(value);
    }
}
