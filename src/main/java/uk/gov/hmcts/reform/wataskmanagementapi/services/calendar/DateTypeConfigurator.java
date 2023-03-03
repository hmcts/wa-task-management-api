package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DateTypeConfigurator {

    public static final String CALCULATED_DATES = "calculatedDates";
    public static final List<DateTypeObject> DEFAULT_DATE_TYPES = Arrays.stream(DateType.values())
        .filter(d -> d != DateType.CALCULATED_DATES)
        .sorted(Comparator.comparing(DateType::getOrder))
        .map(d -> new DateTypeObject(d, d.getType()))
        .toList();

    public static final List<DateTypeObject> MANDATORY_DATE_TYPES = Arrays.stream(DateType.values())
        .filter(d -> d != DateType.CALCULATED_DATES)
        .filter(d -> d != DateType.INTERMEDIATE_DATE)
        .sorted(Comparator.comparing(DateType::getOrder))
        .map(d -> new DateTypeObject(d, d.getType()))
        .toList();
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDates(
        List<ConfigurationDmnEvaluationResponse> dmnConfigurationResponses,
        boolean initiationDueDateFound,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes) {

        List<DateTypeObject> calculationOrder = readCalculationOrder(dmnConfigurationResponses);
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> calculatedResponses
            = new AtomicReference<>(new ArrayList<>());

        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configurationResponses
            = new AtomicReference<>(new ArrayList<>(dmnConfigurationResponses));

        calculationOrder
            .forEach(dateTypeObject -> {
                List<ConfigurationDmnEvaluationResponse> dateProperties = configurationResponses.get().stream()
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
                    calculatedResponses,
                    taskAttributes,
                    configurationResponses.get()
                );
                log.info("{} based in configuration is as {}", dateTypeObject.dateTypeName, dateTypeResponse);
                Optional.ofNullable(dateTypeResponse).ifPresent(r -> calculatedResponses.get().add(r));
                filterOutOldValueAndAddDateType(configurationResponses, dateTypeObject, dateTypeResponse);
            });

        return calculatedResponses.get();
    }

    private static ConfigurationDmnEvaluationResponse getDefaultValue(
        DateType dateType,
        List<ConfigurationDmnEvaluationResponse> configResponses) {

        Optional<ConfigurationDmnEvaluationResponse> dueDate = configResponses.stream()
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

    private List<DateTypeObject> readCalculationOrder(List<ConfigurationDmnEvaluationResponse> configResponses) {

        Optional<List<DateTypeObject>> dateTypes = configResponses.stream()
            .filter(r1 -> r1.getName().getValue().equals(CALCULATED_DATES))
            .reduce((a, b) -> b)
            .map(r -> Arrays.stream(r.getValue().getValue().split(","))
                .map(s -> new DateTypeObject(DateType.from(s), s))
                .collect(Collectors.toList()));

        if (dateTypes.isPresent()) {
            List<DateTypeObject> filtered = new ArrayList<>(dateTypes.get()).stream()
                .filter(d -> INTERMEDIATE_DATE != d.dateType)
                .toList();
            if (!new HashSet<>(filtered).containsAll(MANDATORY_DATE_TYPES)) {
                throw new RuntimeException("Calculates dates misses mandatory date types.");
            }

            if (!filtered.equals(MANDATORY_DATE_TYPES)) {
                throw new RuntimeException("Calculates dates are not in correct order.");
            }

            return dateTypes.get();
        }
        return dateTypes.orElse(DEFAULT_DATE_TYPES);
    }

    private ConfigurationDmnEvaluationResponse getResponseFromDateCalculator(
        boolean isReconfigureRequest,
        DateTypeObject dateTypeObject,
        List<ConfigurationDmnEvaluationResponse> dateProperties,
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> calculatedConfigurations,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> configurationResponses) {
        Optional<DateCalculator> dateCalculator
            = getDateCalculator(dateProperties, dateTypeObject, isReconfigureRequest);
        if (dateCalculator.isPresent()) {
            return dateCalculator.get().calculateDate(
                dateProperties,
                dateTypeObject,
                isReconfigureRequest,
                taskAttributes,
                calculatedConfigurations.get()
            );
        } else {
            return isReconfigureRequest ? null : getDefaultValue(dateTypeObject.dateType, configurationResponses);
        }
    }

    private Optional<DateCalculator> getDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        List<DateCalculator> calculators = dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, dateTypeObject, isReconfigureRequest))
            .toList();
        if (calculators.size() > 1) {
            throw new RuntimeException("Origin dates have multiple occurrence, Date type can't be calculated.");
        }
        return calculators.stream().findFirst();
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
