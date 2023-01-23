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
public class PriorityDateTimeReCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest
    ) {
        ConfigurationDmnEvaluationResponse priorityDateTime = getProperty(priorityDateProperties, PRIORITY_DATE_TIME);
        ConfigurationDmnEvaluationResponse priorityDate = getProperty(priorityDateProperties, PRIORITY_DATE.getType());
        ConfigurationDmnEvaluationResponse priorityDateOrigin = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_ORIGIN
        );
        return PRIORITY_DATE == dateType
            && isReconfigureRequest
            && (Optional.ofNullable(priorityDate).isEmpty()
            || priorityDate.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && (Optional.ofNullable(priorityDateOrigin).isEmpty()
            || priorityDateOrigin.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && Optional.ofNullable(priorityDateTime).isPresent()
            && priorityDateTime.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType) {
        var priorityDateTimeResponse = getProperty(priorityDateProperties, PRIORITY_DATE_TIME);
        LocalDateTime dateTime = addTimeToDate(priorityDateTimeResponse, DEFAULT_DATE);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }
}
