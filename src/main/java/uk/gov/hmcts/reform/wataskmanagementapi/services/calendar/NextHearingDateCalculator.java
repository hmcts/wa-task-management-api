package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
//TODO Dummy implementation based on DueDate
public class NextHearingDateCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE.getType())).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType) {
        var nextHearingDateResponse = getProperty(dueDateProperties, NEXT_HEARING_DATE.getType());
        var nextHearingDateTimeResponse = getProperty(dueDateProperties, NEXT_HEARING_DATE_TIME);
        return calculatedDate(dateType, nextHearingDateResponse, nextHearingDateTimeResponse);
    }
}
