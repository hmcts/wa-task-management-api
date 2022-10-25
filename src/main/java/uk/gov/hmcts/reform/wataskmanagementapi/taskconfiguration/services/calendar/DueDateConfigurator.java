package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DateCalculator.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DateCalculator.DUE_DATE_TIME_FORMATTER;

@Slf4j
@Component
public class DueDateConfigurator {

    public static final String IA_JURISDICTION = "IA";
    private final List<DateCalculator> dateCalculators;

    public DueDateConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses, String jurisdiction) {
        List<ConfigurationDmnEvaluationResponse> dueDateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE))
            .collect(Collectors.toList());

        if (dueDateProperties.isEmpty() && IA_JURISDICTION.equals(jurisdiction)) {
            return configResponses;
        }

        AtomicReference<LocalDateTime> dueDate = new AtomicReference<>();
        dueDate.set(DEFAULT_ZONED_DATE_TIME);

        Optional<DateCalculator> dueDateCalculator = getDueDateCalculator(dueDateProperties);
        dueDateCalculator
            .ifPresent((dateCalculator) -> dueDate.getAndSet(dateCalculator.calculateDueDate(dueDateProperties)));

        ConfigurationDmnEvaluationResponse dueDateResponse = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(DUE_DATE))
            .value(CamundaValue.stringValue(dueDate.get().format(DUE_DATE_TIME_FORMATTER)))
            .build();

        List<ConfigurationDmnEvaluationResponse> withoutDueDate = new ArrayList<>(filterOutDueDate(configResponses));
        withoutDueDate.add(dueDateResponse);
        return withoutDueDate;
    }

    private Optional<DateCalculator> getDueDateCalculator(List<ConfigurationDmnEvaluationResponse> configResponses) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses))
            .findFirst();
    }

    private List<ConfigurationDmnEvaluationResponse> filterOutDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses) {
        return configResponses.stream()
            .filter(r -> !r.getName().getValue().contains(DUE_DATE))
            .collect(Collectors.toList());
    }
}
