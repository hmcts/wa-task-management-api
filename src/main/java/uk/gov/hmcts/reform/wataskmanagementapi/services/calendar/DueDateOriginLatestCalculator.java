package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, DUE_DATE.getType())
            && Optional.ofNullable(getProperty(configResponses, DUE_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(configResponses, DUE_DATE_ORIGIN_LATEST, isReconfigureRequest))
            .isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        var configProperty = getProperty(configResponses, DUE_DATE_ORIGIN_LATEST, isReconfigureRequest);
        log.info("Input {}: {}", DUE_DATE_ORIGIN_LATEST, configProperty);
        return getOriginLatestDate(calculatedConfigurations, configProperty, taskAttributes, isReconfigureRequest);
    }
}
