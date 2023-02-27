package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateOriginEarliestCalculator extends DueDateIntervalCalculator {

    public DueDateOriginEarliestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN_EARLIEST, isReconfigureRequest))
            .isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginEarliestDate(
            configResponses,
            getProperty(configResponses, DUE_DATE_ORIGIN_EARLIEST, isReconfigureRequest)
        );
    }
}
