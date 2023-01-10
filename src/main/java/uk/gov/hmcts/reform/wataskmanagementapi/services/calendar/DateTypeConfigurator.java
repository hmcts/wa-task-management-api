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

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

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
        boolean initiationDueDateFound,
        boolean isReconfigureRequest) {

        AtomicReference<List<ConfigurationDmnEvaluationResponse>> responses
            = new AtomicReference<>(new ArrayList<>(configResponses));

        Stream.of(DateType.values())
            .filter(t -> t.equals(DUE_DATE))
            .forEach(dt -> {
                List<ConfigurationDmnEvaluationResponse> dateProperties = configResponses.stream()
                    .filter(r -> r.getName().getValue().contains(dt.getType()))
                    .collect(Collectors.toList());

                if (dateProperties.isEmpty() && initiationDueDateFound) {
                    log.info("initiationDueDateFound for configureDueDate");
                    return;
                }

                ConfigurationDmnEvaluationResponse dateTypeResponse = getDateResponse(
                    isReconfigureRequest,
                    dt,
                    dateProperties
                );
                log.info("{} based in configuration is as {}", dt.getType(), dateTypeResponse);
                filterOutOldValueAndAddDateType(responses, dt, dateTypeResponse);
            });

        return responses.get();
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
