package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class DueDateOriginLatestCalculator extends DueDateIntervalCalculator {

    public DueDateOriginLatestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateType.dateType()
            && Optional.ofNullable(getProperty(configResponses, DUE_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(configResponses, DUE_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
            getProperty(configResponses, DUE_DATE_ORIGIN_LATEST, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest) {
        Optional<LocalDateTime> dueDateLatest = getReferenceDate(configResponses, isReconfigureRequest);
        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(configResponses, isReconfigureRequest);
        return calculateDate(dateType, dateTypeIntervalData, dueDateLatest.orElse(DEFAULT_ZONED_DATE_TIME));
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginLatestDate(
            configResponses,
            getProperty(configResponses, DUE_DATE_ORIGIN_LATEST, isReconfigureRequest)
        );
    }
}
