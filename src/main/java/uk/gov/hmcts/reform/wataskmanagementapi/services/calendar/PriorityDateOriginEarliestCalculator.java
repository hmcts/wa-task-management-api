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
public class PriorityDateOriginEarliestCalculator extends DueDateIntervalCalculator {

    public PriorityDateOriginEarliestCalculator(WorkingDayIndicator workingDayIndicator) {
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
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE_ORIGIN_EARLIEST)).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateType dateType) {
        var originEarliestResponse = getProperty(configResponses, PRIORITY_DATE_ORIGIN_EARLIEST);
        Optional<LocalDateTime> dueDateOriginEarliest = getOriginEarliestDate(configResponses, originEarliestResponse);
        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(configResponses, false);
        if (dueDateOriginEarliest.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder()
                .calculatedEarliestDate(dueDateOriginEarliest.get()).build();
        }
        return calculateDate(dateType, dateTypeIntervalData);
    }
}
