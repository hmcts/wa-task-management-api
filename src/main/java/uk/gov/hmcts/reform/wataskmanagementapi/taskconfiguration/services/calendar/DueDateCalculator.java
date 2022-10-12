package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DueDateCalculator {

    private static final String DUE_DATE_PREFIX = "dueDate";
    private static final String DUE_DATE_TIME_PREFIX = "dueDateTime";
    private static final String DUE_DATE_ORIGIN_PREFIX = "dueDateOrigin";
    public static final LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2)
        .withHour(16).withMinute(0).withSecond(0);
    public static final DateTimeFormatter DUE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final DateTimeFormatter DUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String IA_JURISDICTION = "IA";

    private final DueDateIntervalCalculator dueDateIntervalCalculator;

    public DueDateCalculator(DueDateIntervalCalculator dueDateIntervalCalculator) {
        this.dueDateIntervalCalculator = dueDateIntervalCalculator;
    }

    public List<ConfigurationDmnEvaluationResponse> calculateDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses, String jurisdiction) {
        List<ConfigurationDmnEvaluationResponse> dueDateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE_PREFIX))
            .collect(Collectors.toList());

        if (dueDateProperties.isEmpty() && IA_JURISDICTION.equals(jurisdiction)) {
            return configResponses;
        }

        LocalDateTime dueDate = getDueDate(dueDateProperties);
        ConfigurationDmnEvaluationResponse dueDateResponse = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(DUE_DATE_PREFIX))
            .value(CamundaValue.stringValue(dueDate.format(DUE_DATE_TIME_FORMATTER)))
            .build();

        List<ConfigurationDmnEvaluationResponse> responsesWithoutDueDate = filterOutDueDate(configResponses);
        responsesWithoutDueDate.add(dueDateResponse);
        return responsesWithoutDueDate;
    }

    private List<ConfigurationDmnEvaluationResponse> filterOutDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses) {
        return configResponses.stream()
            .filter(r -> !r.getName().getValue().contains(DUE_DATE_PREFIX))
            .collect(Collectors.toList());
    }

    private LocalDateTime getDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        var dueDateResponse = getProperty(dueDateProperties, DUE_DATE_PREFIX);
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME_PREFIX);
        var dueDateOriginResponse = getProperty(dueDateProperties, DUE_DATE_ORIGIN_PREFIX);

        if (dueDateResponse.isPresent()) {
            String dueDate = dueDateResponse.get().getValue().getValue();

            LocalDateTime parsedDueDate = parseDueDateTime(dueDate);

            if (dueDateTimeResponse.isPresent()) {
                String dueDateTime = dueDateTimeResponse.get().getValue().getValue();
                return useDateTime(parsedDueDate, dueDateTime);
            } else {
                if (parsedDueDate.getHour() == 0) {
                    return parsedDueDate.withHour(16).withMinute(0);
                } else {
                    return parsedDueDate;
                }
            }
        } else if (dueDateOriginResponse.isPresent()) {
            return dueDateIntervalCalculator.calculateDueDate(dueDateProperties);
        } else if (dueDateTimeResponse.isPresent()) {
            String dueDateTime = dueDateTimeResponse.get().getValue().getValue();
            return useDateTime(LocalDateTime.now().plusDays(2), dueDateTime);
        }
        return DEFAULT_ZONED_DATE_TIME;
    }

    private static Optional<ConfigurationDmnEvaluationResponse> getProperty(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, String dueDatePrefix) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dueDatePrefix))
            .reduce((a, b) -> b);
    }

    private static LocalDateTime parseDueDateTime(String dueDate) {
        if (dateContainsTime(dueDate)) {
            return LocalDateTime.parse(
                dueDate,
                DUE_DATE_TIME_FORMATTER
            );
        } else {
            return LocalDate.parse(
                dueDate,
                DUE_DATE_FORMATTER
            ).atStartOfDay();
        }
    }

    private static boolean dateContainsTime(String dueDate) {
        return dueDate.contains("T");
    }

    private static LocalDateTime useDateTime(LocalDateTime date, String dueDateTime) {

        List<String> split = Arrays.asList(dueDateTime.replace("T", "").trim().split(":"));
        return date
            .with(ChronoField.HOUR_OF_DAY, Long.parseLong(split.get(0)))
            .with(ChronoField.MINUTE_OF_HOUR, Long.parseLong(split.get(1)));
    }
}
