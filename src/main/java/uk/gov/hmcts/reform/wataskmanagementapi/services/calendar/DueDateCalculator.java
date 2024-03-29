package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigure) {

        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType(), isReconfigure)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType, boolean isReconfigureRequest,
        Map<String, Object> taskAttributes, List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        var dueDateResponse = getProperty(configResponses, DUE_DATE.getType(), isReconfigureRequest);
        log.info("Input {}: {}", DUE_DATE.getType(), dueDateResponse);
        var dueDateTimeResponse = getProperty(configResponses, DUE_DATE_TIME, isReconfigureRequest);
        return calculatedDate(dateType, dueDateResponse, dueDateTimeResponse, isReconfigureRequest);
    }

    protected ConfigurationDmnEvaluationResponse calculatedDate(
        DateTypeObject dateType,
        ConfigurationDmnEvaluationResponse dueDateResponse,
        ConfigurationDmnEvaluationResponse dueDateTimeResponse, boolean isReconfigureRequest) {
        LocalDateTime calculatedDate = calculatedDate(dueDateResponse, dueDateTimeResponse);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.dateTypeName()))
            .value(CamundaValue.stringValue(dateType.dateType().getDateTimeFormatter().format(calculatedDate)))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
    }

    private LocalDateTime calculatedDate(ConfigurationDmnEvaluationResponse dueDateResponse,
                                         ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        if (Optional.ofNullable(dueDateTimeResponse).isPresent()) {
            return calculateDueDateFrom(dueDateResponse, dueDateTimeResponse);
        } else {
            return calculateDueDateFrom(dueDateResponse);
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        LocalDateTime parsedDueDate = parseDateTime(dueDate);
        log.debug("calculateDueDateFrom parse date time {}: {}", dueDate, parsedDueDate);
        if (parsedDueDate.getHour() == 0 && parsedDueDate.getMinute() == 0) {
            return parsedDueDate.withHour(16).withMinute(0);
        } else {
            return parsedDueDate;
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse,
                                               ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        LocalDateTime parsedDueDate = parseDateTime(dueDate);
        log.debug("calculateDueDateFrom parse date time {}: {}", dueDate, parsedDueDate);
        return addTimeToDate(dueDateTimeResponse, parsedDueDate);

    }
}
