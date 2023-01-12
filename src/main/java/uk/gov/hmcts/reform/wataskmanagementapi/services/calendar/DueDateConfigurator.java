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

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DEFAULT_ZONED_DATE_TIME;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateCalculator.DUE_DATE_TIME_FORMATTER;

@Slf4j
@Component
public class DueDateConfigurator {

    public static final String IA_JURISDICTION = "IA";
    private final List<DateCalculator> dateCalculators;

    public DueDateConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean initiationDueDateFound, boolean isReconfigureRequest) {

        List<ConfigurationDmnEvaluationResponse> dueDateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE))
            .collect(Collectors.toList());

        if (dueDateProperties.isEmpty() && initiationDueDateFound) {
            log.info("initiationDueDateFound for configureDueDate");
            return configResponses;
        }

        AtomicReference<LocalDateTime> dueDate = new AtomicReference<>();
        if (!isReconfigureRequest) {
            dueDate.set(DEFAULT_ZONED_DATE_TIME);
        }

        Optional<DateCalculator> dueDateCalculator = getDueDateCalculator(dueDateProperties, isReconfigureRequest);
        dueDateCalculator
            .ifPresent(dateCalculator -> dueDate.getAndSet(dateCalculator.calculateDueDate(dueDateProperties)));

        List<ConfigurationDmnEvaluationResponse> withoutDueDate = new ArrayList<>(filterOutDueDate(configResponses));

        LocalDateTime dateTime = dueDate.get();
        if (dateTime != null) {
            ConfigurationDmnEvaluationResponse dueDateResponse = ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue(DUE_DATE))
                .value(CamundaValue.stringValue(dateTime.format(DUE_DATE_TIME_FORMATTER)))
                .build();

            log.info("Due date set in configuration is as {}", dueDateResponse);
            withoutDueDate.add(dueDateResponse);
        }
        return withoutDueDate;
    }

    private Optional<DateCalculator> getDueDateCalculator(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                          boolean isReconfigureRequest) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, isReconfigureRequest))
            .findFirst();
    }

    private List<ConfigurationDmnEvaluationResponse> filterOutDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses) {
        return configResponses.stream()
            .filter(r -> !r.getName().getValue().contains(DUE_DATE))
            .collect(Collectors.toList());
    }
}
