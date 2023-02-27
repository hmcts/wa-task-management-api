package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateOriginEarliestCalculator extends IntermediateDateIntervalCalculator {

    public IntermediateDateOriginEarliestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse intermediateOrigin = getProperty(
            dueDateProperties,
            dateTypeName + ORIGIN_SUFFIX,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse intermediateOriginEarliest = getProperty(
            dueDateProperties,
            dateTypeName + ORIGIN_EARLIEST_SUFFIX,
            isReconfigureRequest
        );
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(intermediateOrigin).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(intermediateOriginEarliest).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(String dateTypeName,
                                                       List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginEarliestDate(
            configResponses,
            getProperty(configResponses, dateTypeName + ORIGIN_EARLIEST_SUFFIX, isReconfigureRequest)
        );
    }
}
