package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
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
        List<ConfigurationDmnEvaluationResponse> configResponses, String jurisdiction) {

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
            dateValue.set(dt.getDefaultTime());

            Optional<DateCalculator> dueCalculator = getDueDateCalculator(dateProperties, dt);
            dueCalculator.ifPresent(calculator -> dateValue.getAndSet(calculator.calculateDate(dateProperties)));

            ConfigurationDmnEvaluationResponse dateTypeResponse = ConfigurationDmnEvaluationResponse
                .builder()
                .name(CamundaValue.stringValue(dt.getType()))
                .value(CamundaValue.stringValue(dateValue.get().format(dt.getDateTimeFormatter())))
                .build();

            log.info("Due date set in configuration is as {}", dateTypeResponse);
            filterOutOldValueAndAddDateType(responses, dt, dateTypeResponse);
        });

        return responses.get();
    }

    private Optional<DateCalculator> getDueDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses, DateType dateType) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, dateType))
            .findFirst();
    }

    private List<ConfigurationDmnEvaluationResponse> filterOutOldValueAndAddDateType(
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses,
        DateType dateType,
        ConfigurationDmnEvaluationResponse dateTypeResponse) {
        List<ConfigurationDmnEvaluationResponse> filtered = configResponses.get().stream()
            .filter(r -> !r.getName().getValue().contains(dateType.getType()))
            .collect(Collectors.toList());

        filtered.add(dateTypeResponse);
        return configResponses.getAndSet(filtered);
    }
}
