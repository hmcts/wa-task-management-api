package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateOriginEarliestCalculator extends PriorityDateIntervalCalculator {

    public PriorityDateOriginEarliestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return PRIORITY_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(configResponses, PRIORITY_DATE_ORIGIN, isReconfigureRequest))
            .isEmpty()
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, PRIORITY_DATE.getType())
            && Optional.ofNullable(getProperty(configResponses, PRIORITY_DATE_ORIGIN_EARLIEST, isReconfigureRequest))
            .isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes) {
        return getOriginEarliestDate(
            configResponses,
            getProperty(configResponses, PRIORITY_DATE_ORIGIN_EARLIEST, isReconfigureRequest),
            taskAttributes,
            isReconfigureRequest
        );
    }
}
