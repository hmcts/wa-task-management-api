package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN, isReconfigureRequest))
            .isEmpty()
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, NEXT_HEARING_DATE.getType())
            && Optional.ofNullable(getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_REF, isReconfigureRequest))
            .isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        var configProperty = getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_REF, isReconfigureRequest);
        log.info("Input {}: {}", NEXT_HEARING_DATE_ORIGIN_REF, configProperty);
        return getOriginRefDate(calculatedConfigurations, configProperty, taskAttributes, isReconfigureRequest);
    }
}
