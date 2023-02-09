package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateIntervalReCalculator extends PriorityDateIntervalCalculator {

    public PriorityDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        var priorityDateOrigin = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN);
        var priorityDate = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE.getType());
        return PRIORITY_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(priorityDateOrigin).isPresent()
            && Optional.ofNullable(priorityDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
            DateTypeConfigurator.DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> priorityDateProperties) {
        return calculateDate(dateType, readDateTypeOriginFields(priorityDateProperties, true));
    }
}
