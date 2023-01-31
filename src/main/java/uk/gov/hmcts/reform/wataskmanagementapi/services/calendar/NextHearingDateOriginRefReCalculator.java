package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateOriginRefReCalculator extends NextHearingDateIntervalCalculator {

    public NextHearingDateOriginRefReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, NEXT_HEARING_DATE_ORIGIN)).isEmpty()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, NEXT_HEARING_DATE.getType())).isEmpty()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, NEXT_HEARING_DATE_ORIGIN_REF))
            .isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateType dateType) {
        Optional<LocalDateTime> dueDateOriginRef = getOriginRefDate(
            nextHearingDateProperties,
            getReConfigurableProperty(nextHearingDateProperties, NEXT_HEARING_DATE_ORIGIN_REF)
        );

        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(nextHearingDateProperties, true);
        if (dueDateOriginRef.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder().calculatedRefDate(dueDateOriginRef.get()).build();
        }
        return calculateDate(dateType, dateTypeIntervalData);
    }
}
