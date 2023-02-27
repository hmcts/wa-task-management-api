package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateOriginRefCalculator extends NextHearingDateIntervalCalculator {

    public NextHearingDateOriginRefCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE_ORIGIN,
                                               isReconfigureRequest
        )).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE.getType(),
                                               isReconfigureRequest
        )).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE_ORIGIN_REF,
                                               isReconfigureRequest
        )).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginRefDate(
            configResponses,
            getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_REF, isReconfigureRequest)
        );
    }
}
