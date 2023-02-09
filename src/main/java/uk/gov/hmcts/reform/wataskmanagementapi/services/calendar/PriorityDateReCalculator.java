package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateReCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        var priorityDate = getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE.getType());
        return PRIORITY_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(priorityDate).isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> priorityDateProperties) {
        return calculatedDate(
            dateType,
            getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE.getType()),
            getReConfigurableProperty(priorityDateProperties, PRIORITY_DATE_TIME)
        );
    }
}
