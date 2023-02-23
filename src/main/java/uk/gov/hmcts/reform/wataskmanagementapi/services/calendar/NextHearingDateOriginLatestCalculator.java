package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class NextHearingDateOriginLatestCalculator extends NextHearingDateIntervalCalculator {

    public NextHearingDateOriginLatestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateType.dateType()
            && Optional.ofNullable(
            getProperty(configResponses, NEXT_HEARING_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
            getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
            getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_LATEST, isReconfigureRequest)).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginLatestDate(
            configResponses,
            getProperty(configResponses, NEXT_HEARING_DATE_ORIGIN_LATEST, isReconfigureRequest)
        );
    }
}
