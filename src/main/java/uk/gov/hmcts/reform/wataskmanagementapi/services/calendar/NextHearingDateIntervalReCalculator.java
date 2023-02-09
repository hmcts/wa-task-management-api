package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateIntervalReCalculator extends NextHearingDateIntervalCalculator {

    public NextHearingDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
                            DateTypeObject dateTypeObject,
                            boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = getReConfigurableProperty(
            nextHearingDateProperties,
            NEXT_HEARING_DATE_ORIGIN
        );
        ConfigurationDmnEvaluationResponse nextHearingDate = getReConfigurableProperty(
            nextHearingDateProperties,
            NEXT_HEARING_DATE.getType()
        );
        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(nextHearingDateOrigin).isPresent()
            && Optional.ofNullable(nextHearingDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties) {
        return calculateDate(dateType, readDateTypeOriginFields(nextHearingDateProperties, true));
    }
}
