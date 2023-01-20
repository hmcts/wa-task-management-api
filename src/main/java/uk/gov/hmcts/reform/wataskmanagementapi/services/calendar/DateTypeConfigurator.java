package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DateTypeConfigurator {

    public static final String IA_JURISDICTION = "IA";
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean initiationDueDateFound,
        boolean isReconfigureRequest) {

        AtomicReference<List<ConfigurationDmnEvaluationResponse>> responses
            = new AtomicReference<>(new ArrayList<>(configResponses));

        Stream.of(DateType.values())
            .forEach(dateType -> {
                List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
                    .filter(r -> r.getName().getValue().contains(dateType.getType()))
                    .collect(Collectors.toList());

                if (dateProperties.isEmpty() && initiationDueDateFound) {
                    log.info("initiationDueDateFound for configureDueDate");
                    return;
                }

                ConfigurationDmnEvaluationResponse dateTypeResponse = getResponseFromDateCalculator(
                    isReconfigureRequest,
                    dateType,
                    dateProperties,
                    responses
                );
                log.info("{} based in configuration is as {}", dateType.getType(), dateTypeResponse);
                filterOutOldValueAndAddDateType(responses, dateType, dateTypeResponse);
            });

        return responses.get();
    }

    private ConfigurationDmnEvaluationResponse getResponseFromDateCalculator(
        boolean isReconfigureRequest,
        DateType dateType,
        List<ConfigurationDmnEvaluationResponse> dateProperties,
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses) {
        Optional<DateCalculator> dateCalculator = getDateCalculator(dateProperties, dateType, isReconfigureRequest);
        if (dateCalculator.isPresent()) {
            return dateCalculator.get().calculateDate(dateProperties, dateType);
        } else {
            return isReconfigureRequest ? null : getDefaultValue(dateType, configResponses);
        }
    }

    private static ConfigurationDmnEvaluationResponse getDefaultValue(
        DateType dateType,
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses) {

        Optional<ConfigurationDmnEvaluationResponse> dueDate = configResponses.get().stream()
            .filter(r -> r.getName().getValue().equals(DateType.DUE_DATE.getType()))
            .findFirst();

        if (dateType == PRIORITY_DATE && dueDate.isPresent()) {
            return ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue(PRIORITY_DATE.getType()))
                .value(dueDate.get().getValue())
                .build();
        }

        return dateType.getDefaultTime() == null
            ? null
            : ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateType.getDefaultTime())))
            .build();
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
