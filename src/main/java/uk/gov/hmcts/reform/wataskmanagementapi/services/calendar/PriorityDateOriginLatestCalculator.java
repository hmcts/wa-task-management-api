package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class PriorityDateOriginLatestCalculator extends PriorityDateIntervalCalculator {

    public PriorityDateOriginLatestCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateType dateType,
        boolean isReconfigureRequest) {

        return PRIORITY_DATE == dateType
            && Optional.ofNullable(
                getProperty(configResponses, PRIORITY_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
                getProperty(configResponses, PRIORITY_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(
                getProperty(configResponses, PRIORITY_DATE_ORIGIN_LATEST, isReconfigureRequest)).isPresent();
    }

    @Override
    protected LocalDateTime readPriorityDateOrigin(List<ConfigurationDmnEvaluationResponse> configResponses,
                                              boolean reconfigure) {
        ConfigurationDmnEvaluationResponse originLatestResponse = getProperty(configResponses,
            PRIORITY_DATE_ORIGIN_LATEST);

        List<DateType> originDateTypes = Arrays.stream(originLatestResponse.getValue().getValue().split(","))
            .map(s -> DateType.from(s).orElseThrow()).collect(Collectors.toList());

        return configResponses.stream()
            .filter(r -> DateType.from(r.getName().getValue()).isPresent()
                && originDateTypes.contains(DateType.from(r.getName().getValue()).get()))
            .map(r -> LocalDateTime.parse(r.getValue().getValue(), DATE_TIME_FORMATTER))
            .max(LocalDateTime::compareTo)
            .get();
    }
}
