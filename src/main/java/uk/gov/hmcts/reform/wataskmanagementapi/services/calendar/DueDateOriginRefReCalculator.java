package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateOriginRefReCalculator extends DueDateIntervalCalculator {

    public DueDateOriginRefReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN)).isEmpty()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, DUE_DATE.getType())).isEmpty()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN_REF)).isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> configResponses) {
        ConfigurationDmnEvaluationResponse originRefResponse
            = getReConfigurableProperty(configResponses, DUE_DATE_ORIGIN_REF);
        Optional<LocalDateTime> dueDateOriginRef = getOriginRefDate(configResponses, originRefResponse);
        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(configResponses, true);
        if (dueDateOriginRef.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder().calculatedRefDate(dueDateOriginRef.get()).build();
        }
        return calculateDate(dateType, dateTypeIntervalData);
    }
}
