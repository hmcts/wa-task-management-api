package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DateTypeConfigurator {

    public static final String IA_JURISDICTION = "IA";
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses, String jurisdiction) {
        List<ConfigurationDmnEvaluationResponse> withoutDateNames = new ArrayList<>(configResponses);
        Arrays.stream(DateName.values()).filter(n -> n.getName().contains(DateName.DUE_DATE.getName()))
            .forEach(dateName -> {
                List<ConfigurationDmnEvaluationResponse> dateNameProperties = configResponses.stream()
                    .filter(r -> r.getName().getValue().contains(dateName.getName()))
                    .collect(Collectors.toList());

                if (dateNameProperties.isEmpty() && jurisdiction.equals(IA_JURISDICTION)) {
                    return;
                }
                AtomicReference<LocalDateTime> dateValue = new AtomicReference<>();
                dateValue.set(dateName.getDefaultTime());

                Optional<DateCalculator> dueCalculator = getDueDateCalculator(dateNameProperties, dateName);
                dueCalculator.ifPresent(calculator -> dateValue.getAndSet(calculator.calculateDateName(
                    dateNameProperties)));

                ConfigurationDmnEvaluationResponse dateNameResponse = ConfigurationDmnEvaluationResponse
                    .builder()
                    .name(CamundaValue.stringValue(dateName.getName()))
                    .value(CamundaValue.stringValue(dateValue.get().format(dateName.getDateTimeFormatter())))
                    .build();

                log.info("Due date set in configuration is as {}", dateNameResponse);
                filterOutDateName(withoutDateNames, dateName);
                withoutDateNames.add(dateNameResponse);
            });
        return withoutDateNames;
    }

    private Optional<DateCalculator> getDueDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses, DateName dateName) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.hasDateName(dateName))
            .filter(dateCalculator -> dateCalculator.supports(configResponses))
            .findFirst();
    }

    private void filterOutDateName(List<ConfigurationDmnEvaluationResponse> configResponses, DateName dateName) {
        configResponses.removeIf(r -> r.getName().getValue().contains(dateName.getName()));
    }
}
