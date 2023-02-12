package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateOriginEarliestCalculator extends NextHearingDateIntervalCalculator {

    public NextHearingDateOriginEarliestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse nextHearingDateOriginEarliest = getProperty(
            dueDateProperties,
            NEXT_HEARING_DATE_ORIGIN_EARLIEST,
            isReconfigureRequest
        );
        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE_ORIGIN, isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE.getType(), isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(nextHearingDateOriginEarliest).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses, DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        var originEarliestResponse
            = getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_EARLIEST, isReconfigureRequest);
        Optional<LocalDateTime> dueDateOriginEarliest = getOriginEarliestDate(configResponses, originEarliestResponse);
        var dateTypeIntervalData = readDateTypeOriginFields(configResponses, isReconfigureRequest);
        if (dueDateOriginEarliest.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder()
                .calculatedEarliestDate(dueDateOriginEarliest.get()).build();
        }
        return calculateDate(dateTypeObject, dateTypeIntervalData);
    }
}
