package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class IntermediateDateOriginLatestCalculator extends IntermediateDateIntervalCalculator {

    public IntermediateDateOriginLatestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest) {

        String dateTypeName = dateType.dateTypeName();
        return INTERMEDIATE_DATE == dateType.dateType()
            && Optional.ofNullable(getProperty(configResponses, dateTypeName, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(configResponses, dateTypeName + ORIGIN_SUFFIX, isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(
            getProperty(configResponses, dateTypeName + ORIGIN_LATEST_SUFFIX, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName();
        Optional<LocalDateTime> intermediateDateOriginEarliest
            = getReferenceDate(dateTypeName, configResponses, isReconfigureRequest);
        return intermediateDateOriginEarliest.map(localDateTime -> calculateDate(
                dateTypeObject,
                readDateTypeOriginFields(dateTypeName, configResponses, isReconfigureRequest),
                localDateTime
            ))
            .orElse(null);
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(String dateTypeName,
                                                       List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginLatestDate(
            configResponses,
            getProperty(configResponses, dateTypeName + ORIGIN_LATEST_SUFFIX, isReconfigureRequest)
        );
    }
}
