package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateTimeReCalculator extends DueDateTimeCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest
    ) {
        var priorityDateTime = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_TIME);
        var priorityDate = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE.getType());
        var priorityDateOrigin = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN);
        return PRIORITY_DATE == dateType
            && isReconfigureRequest
            && Optional.ofNullable(priorityDate).isEmpty()
            && Optional.ofNullable(priorityDateOrigin).isEmpty()
            && Optional.ofNullable(priorityDateTime).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType) {
        return calculatedDate(dateType, getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_TIME));
    }
}
