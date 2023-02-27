package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

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
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        return PRIORITY_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE.getType(), isReconfigureRequest))
            .isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateTypeObject dateType, boolean isReconfigureRequest) {
        var priorityDateResponse = getProperty(priorityDateProperties, PRIORITY_DATE.getType(), isReconfigureRequest);
        var priorityDateTimeResponse = getProperty(priorityDateProperties, PRIORITY_DATE_TIME, isReconfigureRequest);
        return calculatedDate(dateType, priorityDateResponse, priorityDateTimeResponse);
    }
}
