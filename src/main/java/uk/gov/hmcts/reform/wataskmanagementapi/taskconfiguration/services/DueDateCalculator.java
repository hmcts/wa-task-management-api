package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

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
    private static final String DUE_DATE_TME_PREFIX = "dueDateTime";
    public static final LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2).withHour(16);
    public static final DateTimeFormatter DUE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final DateTimeFormatter DUE_DATE_TIME_FORMATTER_WITHOUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ConfigurationDmnEvaluationResponse> calculateDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses) {
        List<ConfigurationDmnEvaluationResponse> dueDateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE_PREFIX))
            .collect(Collectors.toList());

        if (dueDateProperties.isEmpty()) {
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
        Optional<ConfigurationDmnEvaluationResponse> dueDateResponse = dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(DUE_DATE_PREFIX))
            .reduce((a, b) -> b);

        Optional<ConfigurationDmnEvaluationResponse> dueDateTimeResponse = dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(DUE_DATE_TME_PREFIX))
            .reduce((a, b) -> b);

        if (dueDateResponse.isPresent()) {
            String dueDate = dueDateResponse.get().getValue().getValue();

            LocalDateTime parsedDueDate = parseDueDateTime(dueDate);

            if (dueDateTimeResponse.isPresent()) {
                String dueDateTime = dueDateTimeResponse.get().getValue().getValue();
                return useDateTime(parsedDueDate, dueDateTime);
            } else {
                if (parsedDueDate.getHour() == 0) {
                    return parsedDueDate.withHour(16);
                } else {
                    return parsedDueDate;
                }
            }
        } else if (dueDateTimeResponse.isPresent()) {
            String dueDateTime = dueDateTimeResponse.get().getValue().getValue();
            return useDateTime(LocalDateTime.now().plusDays(2), dueDateTime);
        } else {
            return DEFAULT_ZONED_DATE_TIME;
        }
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
                DUE_DATE_TIME_FORMATTER_WITHOUT_TIME
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
