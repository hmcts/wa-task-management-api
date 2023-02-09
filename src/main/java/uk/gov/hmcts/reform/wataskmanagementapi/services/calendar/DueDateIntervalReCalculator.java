package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateIntervalReCalculator extends DueDateIntervalCalculator {

    public DueDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        var dueDateOrigin = getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN);
        var dueDate = getReConfigurableProperty(dueDateProperties, DUE_DATE.getType());
        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(dueDateOrigin).isPresent()
            && Optional.ofNullable(dueDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(DateTypeObject dateType,
                                                            List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculateDate(dateType, readDateTypeOriginFields(configResponses, true));
    }
}
