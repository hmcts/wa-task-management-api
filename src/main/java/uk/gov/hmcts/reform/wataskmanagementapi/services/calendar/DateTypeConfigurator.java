package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class DateTypeConfigurator {

    public static final String IA_JURISDICTION = "IA";
    public static final String CALCULATED_DATES = "calculatedDates";
    private final List<DateCalculator> dateCalculators;

    public DateTypeConfigurator(List<DateCalculator> dateCalculators) {
        this.dateCalculators = dateCalculators;
    }

    public List<ConfigurationDmnEvaluationResponse> configureDates(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean initiationDueDateFound,
        boolean isReconfigureRequest) {

        List<DateType> calculationOrder = readCalculationOrder(configResponses);
        AtomicReference<List<ConfigurationDmnEvaluationResponse>> responses
            = new AtomicReference<>(new ArrayList<>(configResponses));


        calculationOrder
            .forEach(dt -> {
                switch (dt) {
                    case DUE_DATE:
                        Optional<ConfigurationDmnEvaluationResponse> dueDate = calculateDueDate(configResponses,
                                                                                                initiationDueDateFound,
                                                                                                isReconfigureRequest);
                        dueDate.ifPresent(d -> filterOutOldValueAndAddDateType(responses, DUE_DATE, d));
                        break;
                    case NEXT_HEARING_DATE:
                    case PRIORITY_DATE:
                        Optional<ConfigurationDmnEvaluationResponse> calculatedDate = calculateDate(configResponses,
                                                                                                dt,
                                                                                                isReconfigureRequest);
                        calculatedDate.ifPresent(d -> filterOutOldValueAndAddDateType(responses, dt, d));
                        break;
                }
            });

        return responses.get();
    }

    private Optional<ConfigurationDmnEvaluationResponse> calculateDueDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                                      boolean initiationDueDateFound,
                                                                      boolean isReconfigureRequest) {
        List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE.getType()))
            .collect(Collectors.toList());

        if (dateProperties.isEmpty() && initiationDueDateFound) {
            log.info("initiationDueDateFound for configureDueDate");
            return Optional.empty();
        }

        ConfigurationDmnEvaluationResponse dateTypeResponse = getDateResponse(
            isReconfigureRequest,
            DUE_DATE,
            dateProperties
        );
        log.info("{} based in configuration is as {}", DUE_DATE.getType(), dateTypeResponse);
        return Optional.ofNullable(dateTypeResponse);
    }

    private Optional<ConfigurationDmnEvaluationResponse> calculateDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                                       DateType dateType,
                                                                       boolean isReconfigureRequest) {
        List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(dateType.getType()))
            .collect(Collectors.toList());

        ConfigurationDmnEvaluationResponse dateTypeResponse = getDateResponse(
            isReconfigureRequest,
            dateType,
            dateProperties
        );
        log.info("{} based in configuration is as {}", dateType.getType(), dateTypeResponse);
        return Optional.ofNullable(dateTypeResponse);
    }

    private List<DateType> readCalculationOrder(List<ConfigurationDmnEvaluationResponse> configResponses) {
        Optional<ConfigurationDmnEvaluationResponse> calculatedDates = configResponses.stream()
            .filter(r -> r.getName().getValue().equals(CALCULATED_DATES))
            .findFirst();

        DateType[] defaultOrder = DateType.values();
        Arrays.sort(defaultOrder, Comparator.comparing(DateType::getOrder));

        return calculatedDates.map(r -> Arrays.stream(r.getValue().getValue().split(","))
            .map(s -> DateType.from(s).orElseThrow()).collect(Collectors.toList()))
            .orElseGet(() -> Arrays.asList(defaultOrder));
    }

    private ConfigurationDmnEvaluationResponse getDateResponse(
        boolean isReconfigureRequest,
        DateType dateType,
        List<ConfigurationDmnEvaluationResponse> dateProperties) {
        Optional<DateCalculator> dateCalculator = getDateCalculator(dateProperties, dateType, isReconfigureRequest);
        if (dateCalculator.isPresent()) {
            return dateCalculator.get().calculateDate(dateProperties, dateType);

        } else {
            return isReconfigureRequest
                ? null
                : ConfigurationDmnEvaluationResponse.builder()
                .name(CamundaValue.stringValue(dateType.getType()))
                .value(CamundaValue.stringValue(dateType.getDateTimeFormatter()
                                                    .format(dateType.getDefaultTime()))).build();
        }
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
