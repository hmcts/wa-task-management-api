package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType())).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
            DateType dateType, List<ConfigurationDmnEvaluationResponse> configResponses) {
        var dueDateResponse = getProperty(configResponses, DUE_DATE.getType());
        var dueDateTimeResponse = getProperty(configResponses, DUE_DATE_TIME);
        return calculatedDate(dateType, dueDateResponse, dueDateTimeResponse);
    }

    protected ConfigurationDmnEvaluationResponse calculatedDate(
        DateType dateType,
        ConfigurationDmnEvaluationResponse dueDateResponse,
        ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        LocalDateTime calculatedDate = calculatedDate(dueDateResponse, dueDateTimeResponse);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(calculatedDate)))
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
        if (parsedDueDate.getHour() == 0 && parsedDueDate.getMinute() == 0) {
            return parsedDueDate.withHour(16).withMinute(0);
        } else {
            return parsedDueDate;
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse,
                                       ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        return addTimeToDate(dueDateTimeResponse, parseDateTime(dueDate));
    }
}
