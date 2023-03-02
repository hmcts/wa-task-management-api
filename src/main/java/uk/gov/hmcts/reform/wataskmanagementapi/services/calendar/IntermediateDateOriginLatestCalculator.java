package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, dateTypeName)
            && Optional.ofNullable(getProperty(configResponses, dateTypeName + ORIGIN_SUFFIX, isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(
            getProperty(configResponses, dateTypeName + ORIGIN_LATEST_SUFFIX, isReconfigureRequest)).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
            String dateTypeName,
            List<ConfigurationDmnEvaluationResponse> configResponses,
            boolean isReconfigureRequest,
            Map<String, Object> taskAttributes,
            List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        return getOriginLatestDate(
            calculatedConfigurations,
            getProperty(configResponses, dateTypeName + ORIGIN_LATEST_SUFFIX, isReconfigureRequest),
            taskAttributes,
            isReconfigureRequest
        );
    }
}
