package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateReCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        var nextHearingDate = getReConfigurableProperty(nextHearingDateProperties, NEXT_HEARING_DATE.getType());
        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(nextHearingDate).isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeConfigurator.DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties) {
        return calculatedDate(
            dateType,
            getReConfigurableProperty(nextHearingDateProperties, NEXT_HEARING_DATE.getType()),
            getReConfigurableProperty(nextHearingDateProperties, NEXT_HEARING_DATE_TIME)
        );
    }
}
