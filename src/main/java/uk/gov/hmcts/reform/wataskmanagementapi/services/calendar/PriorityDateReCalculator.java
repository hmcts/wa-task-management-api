package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateReCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse priorityDate = getProperty(priorityDateProperties, PRIORITY_DATE.getType());
        return PRIORITY_DATE == dateType
            && Optional.ofNullable(priorityDate).isPresent()
            && priorityDate.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType) {
        var priorityDateResponse = getProperty(priorityDateProperties, PRIORITY_DATE.getType());
        var priorityDateTimeResponse = getProperty(priorityDateProperties, PRIORITY_DATE_TIME);

        LocalDateTime dateTime = getDateTime(priorityDateResponse, priorityDateTimeResponse);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private LocalDateTime getDateTime(ConfigurationDmnEvaluationResponse priorityDateResponse,
                                      ConfigurationDmnEvaluationResponse priorityDateTimeResponse) {
        if (Optional.ofNullable(priorityDateTimeResponse).isEmpty()
            || priorityDateTimeResponse.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE) {
            return calculatePriorityDateFrom(priorityDateResponse);
        } else {
            return calculatePriorityDateFrom(priorityDateResponse, priorityDateTimeResponse);
        }
    }

    private LocalDateTime calculatePriorityDateFrom(ConfigurationDmnEvaluationResponse priorityDateResponse) {
        String priorityDate = priorityDateResponse.getValue().getValue();
        LocalDateTime parsedPriorityDate = parseDateTime(priorityDate);
        if (parsedPriorityDate.getHour() == 0) {
            return parsedPriorityDate.withHour(16).withMinute(0);
        } else {
            return parsedPriorityDate;
        }
    }

    private LocalDateTime calculatePriorityDateFrom(ConfigurationDmnEvaluationResponse priorityDateResponse,
                                                    ConfigurationDmnEvaluationResponse priorityDateTimeResponse) {
        String priorityDate = priorityDateResponse.getValue().getValue();
        return addTimeToDate(priorityDateTimeResponse, parseDateTime(priorityDate));
    }
}
