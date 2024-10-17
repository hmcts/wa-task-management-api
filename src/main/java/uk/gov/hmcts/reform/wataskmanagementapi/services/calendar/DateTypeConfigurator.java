package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.DateCalculationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.CALCULATED_DATES;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.CyclomaticComplexity"})
public class DateTypeConfigurator {

    private static final List<DateTypeObject> DEFAULT_DATE_TYPES = Arrays.stream(DateType.values())
        .filter(d -> d != CALCULATED_DATES)
        .sorted(Comparator.comparing(DateType::getOrder))
        .map(d -> new DateTypeObject(d, d.getType()))
        .toList();

    private static final List<DateTypeObject> MANDATORY_DATE_TYPES = Arrays.stream(DateType.values())
        .filter(d -> d != CALCULATED_DATES)
        .filter(d -> d != INTERMEDIATE_DATE)
        .sorted(Comparator.comparing(DateType::getOrder))
        .map(d -> new DateTypeObject(d, d.getType()))
        .toList();
    public static final String MANDATORY_DATES_NOT_PROVIDED_IN_CALCULATED_DATES
        = "Mandatory dates are not provided in calculatedDates field."
        + " Must provide (nextHearingDate,dueDate,priorityDate)";
    public static final String MANDATORY_DATES_NOT_IN_REQUIRED_ORDER_IN_CALCULATED_DATES
        = "Mandatory dates are not in required order in calculatedDates field."
        + " Must be (nextHearingDate,dueDate,priorityDate)";
    public static final String AMBIGUOUS_ORIGIN_DATES_PROVIDED
        = "Origin dates have multiple occurrence, Date type can't be calculated.";
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
        log.info(
            "Date Calculation order is {}",
            calculationOrder.stream().map(c -> c.dateTypeName).collect(Collectors.joining(","))
        );
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> calculatedResponses
            = new AtomicReference<>(new ArrayList<>());
        List<ConfigurationDmnEvaluationResponse> configurationResponsesWithoutCalculatedDates
            = dmnConfigurationResponses.stream()
            .filter(r -> !r.getName().getValue().contains(CALCULATED_DATES.getType()))
            .toList();
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configurationResponses
            = new AtomicReference<>(new ArrayList<>(configurationResponsesWithoutCalculatedDates));

        calculationOrder
            .forEach(dateTypeObject -> {
                List<ConfigurationDmnEvaluationResponse> dateProperties = configurationResponses.get().stream()
                    .filter(r -> r.getName().getValue().contains(dateTypeObject.dateTypeName))
                    .collect(Collectors.toList());

                ConfigurationDmnEvaluationResponse dateTypeResponse = getResponseFromDateCalculator(
                    isReconfigureRequest,
                    dateTypeObject,
                    dateProperties,
                    calculatedResponses,
                    taskAttributes,
                    configurationResponses.get(),
                    initiationDueDateFound
                );
                log.info("Calculated value of {} is as {}", dateTypeObject.dateTypeName, dateTypeResponse);
                calculatedResponses.get().add(dateTypeResponse);
                filterOutOldValueAndAddDateType(configurationResponses, dateTypeObject, dateTypeResponse,
                                                isReconfigureRequest);
            });

        return configurationResponses.get();
    }

    private ConfigurationDmnEvaluationResponse getDefaultValueForConfiguration(
        DateTypeObject dateTypeObject,
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean initiationDueDateFound) {

        Optional<ConfigurationDmnEvaluationResponse> dueDate = configResponses.stream()
            .filter(r -> r.getName().getValue().equals(DateType.DUE_DATE.getType()))
            .findFirst();

        DateType dateType = dateTypeObject.dateType;

        if (dateType == PRIORITY_DATE && dueDate.isPresent()) {
            return ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue(PRIORITY_DATE.getType()))
                .value(dueDate.get().getValue())
                .build();
        }

        return dateType.getDefaultDateTime() == null
            ? addEmptyConfiguration(dateTypeObject.dateTypeName)
            : defaultValueBasedOnInitiationDueDate(dateTypeObject, initiationDueDateFound);
    }

    private ConfigurationDmnEvaluationResponse defaultValueBasedOnInitiationDueDate(
        DateTypeObject dateTypeObject, boolean initiationDueDateFound) {
        return initiationDueDateFound
            ? addEmptyConfiguration(dateTypeObject.dateTypeName)
            : defaultValueFromDateType(dateTypeObject);
    }

    private ConfigurationDmnEvaluationResponse defaultValueFromDateType(DateTypeObject dateTypeObject) {
        DateType dateType = dateTypeObject.dateType;
        return ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(dateTypeObject.dateTypeName))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter()
                                                .format(dateType.getDefaultDateTime())))
            .build();
    }

    private ConfigurationDmnEvaluationResponse addEmptyConfiguration(String type) {
        return ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(type))
            .value(CamundaValue.stringValue(""))
            .build();
    }

    private List<DateTypeObject> readCalculationOrder(List<ConfigurationDmnEvaluationResponse> configResponses) {

        Optional<List<DateTypeObject>> dateTypes = configResponses.stream()
            .filter(r1 -> r1.getName().getValue().equals(CALCULATED_DATES.getType()))
            .reduce((a, b) -> b)
            .map(r -> Arrays.stream(r.getValue().getValue().split(","))
                .map(s -> new DateTypeObject(DateType.from(s), s))
                .collect(Collectors.toList()));

        if (dateTypes.isPresent()) {
            List<DateTypeObject> filtered = new ArrayList<>(dateTypes.get()).stream()
                .filter(d -> INTERMEDIATE_DATE != d.dateType)
                .toList();
            if (!new HashSet<>(filtered).containsAll(MANDATORY_DATE_TYPES)) {
                throw new DateCalculationException(MANDATORY_DATES_NOT_PROVIDED_IN_CALCULATED_DATES);
            }

            if (!filtered.equals(MANDATORY_DATE_TYPES)) {
                throw new DateCalculationException(MANDATORY_DATES_NOT_IN_REQUIRED_ORDER_IN_CALCULATED_DATES);
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
        List<ConfigurationDmnEvaluationResponse> configurationResponses,
        boolean initiationDueDateFound) {
        Optional<DateCalculator> dateCalculator = dateProperties.isEmpty()
            ? Optional.empty()
            : getDateCalculator(dateProperties, dateTypeObject, isReconfigureRequest);
        if (dateCalculator.isPresent()) {
            return dateCalculator.get().calculateDate(
                dateProperties,
                dateTypeObject,
                isReconfigureRequest,
                taskAttributes,
                calculatedConfigurations.get()
            );
        } else {
            if (isReconfigureRequest) {
                ConfigurationDmnEvaluationResponse response = addEmptyConfiguration(dateTypeObject.dateTypeName);
                Optional<ConfigurationDmnEvaluationResponse> reconfigureResponse =
                    dateProperties.stream().filter(r -> r.getName().getValue().equals(dateTypeObject.dateTypeName))
                        .findFirst(); //Getting the first result as DateCalculator returns only one value
                if (reconfigureResponse.isPresent()) {
                    response.setCanReconfigure(reconfigureResponse.get().getCanReconfigure());
                }
                return response;
            } else {
                return getDefaultValueForConfiguration(dateTypeObject, configurationResponses, initiationDueDateFound);
            }
        }
    }

    private Optional<DateCalculator> getDateCalculator(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        validateConflictsInDateTypes(configResponses, dateTypeObject.dateTypeName);

        return dateCalculators.stream()
            .filter(dateCalculator -> dateCalculator.supports(configResponses, dateTypeObject, isReconfigureRequest))
            .findFirst();
    }

    private void validateConflictsInDateTypes(List<ConfigurationDmnEvaluationResponse> dateProperties,
                                              String dateTypeName) {

        var multipleDateTypes = dateProperties.stream()
            .filter(e -> e.getName().getValue().contains(dateTypeName + "Origin"))
            .map(e -> e.getName().getValue())
            .distinct()
            .toList();

        boolean hasMultipleOriginTypesForADate = multipleDateTypes.size() > 1;
        if (hasMultipleOriginTypesForADate) {
            throw new DateCalculationException(AMBIGUOUS_ORIGIN_DATES_PROVIDED);
        }
    }

    private void filterOutOldValueAndAddDateType(
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> configResponses,
        DateTypeObject dateTypeObject,
        ConfigurationDmnEvaluationResponse dateTypeResponse,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName;
        List<ConfigurationDmnEvaluationResponse> filtered = configResponses.get().stream()
            .filter(r -> !r.getName().getValue().contains(dateTypeName))
            .collect(Collectors.toList());
        if (dateTypeResponse != null) {
            Optional.of(dateTypeResponse).filter(r -> !r.getValue().getValue().isBlank()).ifPresent(filtered::add);
        }
        //when configureDates is going through calculationOrder, and it's nextHearingDate & the value is empty,
        // add it to the filtered responses and return
        if (dateTypeResponse != null && dateTypeName.equals(NEXT_HEARING_DATE.getType())
            && dateTypeResponse.getValue() != null && dateTypeResponse.getValue().getValue() != null
            && dateTypeResponse.getValue().getValue().isEmpty()
            && isReconfigureRequest && dateTypeResponse.getCanReconfigure() != null
            && dateTypeResponse.getCanReconfigure().getValue()) {
            Optional.of(dateTypeResponse).filter(r -> r.getValue().getValue().isBlank()).ifPresent(filtered::add);
        }
        configResponses.getAndSet(filtered);
    }

    record DateTypeObject(DateType dateType, String dateTypeName) {
    }
}
