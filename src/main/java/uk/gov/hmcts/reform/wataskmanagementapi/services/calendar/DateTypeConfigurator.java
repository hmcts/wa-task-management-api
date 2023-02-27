package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DateTypeConfigurator {

    public static final String CALCULATED_DATES = "calculatedDates";
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
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

        return dateType.getDefaultDateTime() == null
            ? null
            : ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateType.getDefaultDateTime())))
            .build();
    }

    public List<ConfigurationDmnEvaluationResponse> configureDates(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean initiationDueDateFound,
        boolean isReconfigureRequest) {

        List<DateTypeObject> calculationOrder = readCalculationOrder(configResponses);
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> responses
            = new AtomicReference<>(new ArrayList<>(configResponses));

        calculationOrder
            .forEach(dateTypeObject -> {
                List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
                    .filter(r -> r.getName().getValue().contains(dateTypeObject.dateTypeName))
                    .collect(Collectors.toList());

                if (dateProperties.isEmpty() && initiationDueDateFound) {
                    log.info("initiationDueDateFound for configureDueDate");
                    return;
                }

                ConfigurationDmnEvaluationResponse dateTypeResponse = getResponseFromDateCalculator(
                    isReconfigureRequest,
                    dateTypeObject,
                    dateProperties,
                    responses
                );
                log.info("{} based in configuration is as {}", dateTypeObject.dateTypeName, dateTypeResponse);
                filterOutOldValueAndAddDateType(responses, dateTypeObject, dateTypeResponse);
            });

        return responses.get();
    }

    private List<DateTypeObject> readCalculationOrder(List<ConfigurationDmnEvaluationResponse> configResponses) {
        Optional<ConfigurationDmnEvaluationResponse> calculatedDates = configResponses.stream()
            .filter(r -> r.getName().getValue().equals(CALCULATED_DATES))
            .reduce((a, b) -> b);

        DateType[] defaultOrder = DateType.values();
        Arrays.sort(defaultOrder, Comparator.comparing(DateType::getOrder));
        List<DateTypeObject> defaultDateTypeObjects = Arrays.stream(defaultOrder)
            .map(d -> new DateTypeObject(d, d.getType()))
            .toList();

        return calculatedDates.map(r -> Arrays.stream(r.getValue().getValue().split(","))
                .map(s -> new DateTypeObject(DateType.from(s), s)).collect(Collectors.toList()))
            .orElseGet(() -> defaultDateTypeObjects);
    }

    private ConfigurationDmnEvaluationResponse getResponseFromDateCalculator(
        boolean isReconfigureRequest,
        DateTypeObject dateTypeObject,
        List<ConfigurationDmnEvaluationResponse> dateProperties,
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses) {
        Optional<DateCalculator> dateCalculator
            = getDateCalculator(dateProperties, dateTypeObject, isReconfigureRequest);
        if (dateCalculator.isPresent()) {
            return dateCalculator.get().calculateDate(configResponses.get(), dateTypeObject, isReconfigureRequest);
        } else {
            return isReconfigureRequest ? null : getDefaultValue(dateTypeObject.dateType, configResponses);
        }
    }

    private Optional<DateCalculator> getDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, dateTypeObject, isReconfigureRequest))
            .findFirst();
    }

    private void filterOutOldValueAndAddDateType(
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses,
        DateTypeObject dateTypeObject,
        ConfigurationDmnEvaluationResponse dateTypeResponse) {
        List<ConfigurationDmnEvaluationResponse> filtered = configResponses.get().stream()
            .filter(r -> !r.getName().getValue().contains(dateTypeObject.dateTypeName))
            .collect(Collectors.toList());

        Optional.ofNullable(dateTypeResponse).ifPresent(filtered::add);
        configResponses.getAndSet(filtered);
    }

    record DateTypeObject(DateType dateType, String dateTypeName) {
    }
}
