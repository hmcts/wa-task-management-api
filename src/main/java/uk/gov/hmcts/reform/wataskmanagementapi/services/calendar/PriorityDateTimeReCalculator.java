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
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest
    ) {
        var priorityDateTime = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_TIME);
        var priorityDate = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE.getType());
        var priorityDateOrigin = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN);
        return PRIORITY_DATE == dateTypeObject.dateType()
            && isReconfigureRequest
            && Optional.ofNullable(priorityDate).isEmpty()
            && Optional.ofNullable(priorityDateOrigin).isEmpty()
            && Optional.ofNullable(priorityDateTime).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeConfigurator.DateTypeObject dateTypeObject, List<ConfigurationDmnEvaluationResponse> priorityDateProperties) {
        return calculatedDate(dateTypeObject, getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_TIME));
    }
}