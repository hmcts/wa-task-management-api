package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class DueDateCalculator {

    private static final CharSequence DUE_DATE_PREFIX = "dueDate";
    public static final ZonedDateTime DEFAULT_ZONED_DATE_TIME = ZonedDateTime.now().plusDays(2).withHour(16);

    public List<ConfigurationDmnEvaluationResponse> calculateDueDate(
        List<ConfigurationDmnEvaluationResponse> configResponses) {
        Map<String, Object> dueDateProperties = configResponses.stream()
            .filter(r -> r.getName().getValue().contains(DUE_DATE_PREFIX))
            .collect(toMap(r -> r.getName().getValue(), r -> r.getValue().getValue()));

        if (dueDateProperties.isEmpty()) {
            return configResponses;
        }

        ConfigurationDmnEvaluationResponse dueDateResponse = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDate"))
            .value(CamundaValue.stringValue(getDueDate(dueDateProperties)
                                                .format(DateTimeFormatter.ofPattern("yyyy:MM:ddTHH:mi"))))
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

    private ZonedDateTime getDueDate(Map<String, Object> dueDateProperties) {
        if (dueDateProperties.size() > 1) {
            Object dueDate = dueDateProperties.get("dueDate");
            if (Optional.ofNullable(dueDate).isPresent()) {

                ZonedDateTime parsedDueDate = ZonedDateTime.parse((String) dueDate);

                if (parsedDueDate.getHour() == 0) {
                    parsedDueDate = parsedDueDate.withHour(16);
                }

                return parsedDueDate;
            } else {
                Object dueDateTime = dueDateProperties.get("dueDateTime");
                if (Optional.ofNullable(dueDateTime).isPresent()) {
                    return ZonedDateTime.now().plusDays(2)
                        .with(ChronoField.HOUR_OF_DAY, Long.parseLong(dueDateTime.toString().substring(0, 1)))
                        .with(ChronoField.MINUTE_OF_HOUR, Long.parseLong(dueDateTime.toString().substring(2, 3))
                        );
                } else {
                    return DEFAULT_ZONED_DATE_TIME;
                }
            }
        } else {
            return DEFAULT_ZONED_DATE_TIME;
        }
    }
}
