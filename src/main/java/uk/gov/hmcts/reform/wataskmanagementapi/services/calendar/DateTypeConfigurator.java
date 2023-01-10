package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DateTypeConfigurator {

    public static final String IA_JURISDICTION = "IA";
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        String jurisdiction,
        boolean isReconfigureRequest) {

        AtomicReference<List<ConfigurationDmnEvaluationResponse>> responses
            = new AtomicReference<>(new ArrayList<>(configResponses));

        Stream.of(DateType.values()).forEach(dt -> {
            List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
                .filter(r -> r.getName().getValue().contains(dt.getType()))
                .collect(Collectors.toList());

            if (dateProperties.isEmpty() && jurisdiction.equals(IA_JURISDICTION)) {
                return;
            }
            AtomicReference<LocalDateTime> dateValue = new AtomicReference<>();
            if (isReconfigureRequest) {
                filterOutOldValueAndAddDateType(responses, dt, null);
            } else {
                dateValue.set(dt.getDefaultTime());
            }

            Optional<DateCalculator> dateCalculator = getDateCalculator(dateProperties, dt, isReconfigureRequest);
            dateCalculator.ifPresent(calculator -> {
                ConfigurationDmnEvaluationResponse dateTypeResponse = calculator.calculateDate(dateProperties, dt);

                log.info("{} based in configuration is as {}", dt.getType(), dateTypeResponse);
                filterOutOldValueAndAddDateType(responses, dt, dateTypeResponse);
            });
        });

        return responses.get();
    }

    private Optional<DateCalculator> getDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateType dateType,
        boolean isReconfigureRequest) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, dateType, isReconfigureRequest))
            .findFirst();
    }

    private void filterOutOldValueAndAddDateType(
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses,
        DateType dateType,
        ConfigurationDmnEvaluationResponse dateTypeResponse) {
        List<ConfigurationDmnEvaluationResponse> filtered = configResponses.get().stream()
            .filter(r -> !r.getName().getValue().contains(dateType.getType()))
            .collect(Collectors.toList());

        Optional.ofNullable(dateTypeResponse).ifPresent(filtered::add);
        configResponses.getAndSet(filtered);
    }
}
