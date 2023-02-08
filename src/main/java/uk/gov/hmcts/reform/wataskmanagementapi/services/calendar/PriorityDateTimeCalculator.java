package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateTimeCalculator extends DueDateTimeCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return PRIORITY_DATE == dateType
            && Optional.ofNullable(
                getProperty(priorityDateProperties, PRIORITY_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
                getProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
                getProperty(priorityDateProperties, PRIORITY_DATE_TIME, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType, boolean isReconfigureRequest) {
        return calculatedDate(dateType, getProperty(priorityDateProperties, PRIORITY_DATE_TIME, isReconfigureRequest));
    }
}
