package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
//TODO Dummy implementation based on DueDate
public class PriorityDateCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return PRIORITY_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE.getType())).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType) {
        var priorityDateResponse = getProperty(priorityDateProperties, PRIORITY_DATE.getType());
        var priorityDateTimeResponse = getProperty(priorityDateProperties, PRIORITY_DATE_TIME);
        return calculatedDate(dateType, priorityDateResponse, priorityDateTimeResponse);
    }
}
