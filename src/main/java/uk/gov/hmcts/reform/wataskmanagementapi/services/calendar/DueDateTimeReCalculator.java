package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateTimeReCalculator extends DueDateTimeCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest
    ) {
        ConfigurationDmnEvaluationResponse dueDateTime = getReConfigurableProperty(dueDateProperties, DUE_DATE_TIME);
        ConfigurationDmnEvaluationResponse dueDate = getReConfigurableProperty(dueDateProperties, DUE_DATE.getType());
        ConfigurationDmnEvaluationResponse dueDateOrigin
            = getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN);
        return DUE_DATE == dateTypeObject.dateType()
            && isReconfigureRequest
            && Optional.ofNullable(dueDate).isEmpty()
            && Optional.ofNullable(dueDateOrigin).isEmpty()
            && Optional.ofNullable(dueDateTime).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeConfigurator.DateTypeObject dateTypeObject, List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculatedDate(dateTypeObject, getReConfigurableProperty(configResponses, DUE_DATE_TIME));
    }
}
