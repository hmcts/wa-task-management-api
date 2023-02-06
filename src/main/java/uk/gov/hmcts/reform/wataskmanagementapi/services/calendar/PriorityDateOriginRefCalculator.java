package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateOriginRefCalculator extends PriorityDateIntervalCalculator {

    public PriorityDateOriginRefCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        return PRIORITY_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE_ORIGIN)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE.getType())).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE_ORIGIN_REF)).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateType dateType, List<ConfigurationDmnEvaluationResponse> priorityDateProperties) {
        var originRefResponse = getProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN_REF);
        Optional<LocalDateTime> dueDateOriginRef = getOriginRefDate(priorityDateProperties, originRefResponse);
        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(priorityDateProperties, false);
        if (dueDateOriginRef.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder().calculatedRefDate(dueDateOriginRef.get()).build();
        }
        return calculateDate(dateType, dateTypeIntervalData);
    }
}
