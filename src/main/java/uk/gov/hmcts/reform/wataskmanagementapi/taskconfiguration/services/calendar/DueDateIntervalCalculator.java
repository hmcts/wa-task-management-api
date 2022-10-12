package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.dto.DueDateIntervalData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DueDateIntervalCalculator {
    public static final String DUE_DATE_ORIGIN = "dueDateOrigin";
    public static final String DUE_DATE_INTERVAL_DAYS = "dueDateIntervalDays";
    public static final String DUE_DATE_NON_WORKING_CALENDAR = "dueDateNonWorkingCalendar";
    public static final String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final String DUE_DATE_NON_WORKING_DAYS_OF_WEEK = "dueDateNonWorkingDaysOfWeek";
    public static final String DUE_DATE_SKIP_NON_WORKING_DAYS = "dueDateSkipNonWorkingDays";
    public static final String DUE_DATE_MUST_BE_WORKING_DAYS = "dueDateMustBeWorkingDay";
    public static final String DUE_DATE_TIME = "dueDateTime";
    public static final String DEFAULT_DUE_DATE_TIME = "16:00";
    public static final DateTimeFormatter DUE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2)
        .withHour(16).withMinute(0).withSecond(0);
    private final WorkingDayIndicator workingDayIndicator;

    public DueDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        DueDateIntervalData dueDateIntervalData = readDueDateOriginFields(dueDateProperties);

        LocalDateTime dueDate = LocalDateTime.parse(dueDateIntervalData.getDueDateOrigin(), DUE_DATE_TIME_FORMATTER);

        LocalDate localDate = dueDate.toLocalDate();
        if (dueDateIntervalData.isDueDateSkipNonWorkingDays()) {

            for (int counter = 0; counter < dueDateIntervalData.getDueDateIntervalDays(); counter++) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    dueDateIntervalData.getDueDateNonWorkingCalendar(),
                    dueDateIntervalData.getDueDateNonWorkingDaysOfWeek()
                );
            }
        } else {

            localDate = localDate.plusDays(dueDateIntervalData.getDueDateIntervalDays());
            boolean workingDay = workingDayIndicator.isWorkingDay(
                localDate,
                dueDateIntervalData.getDueDateNonWorkingCalendar(),
                dueDateIntervalData.getDueDateNonWorkingDaysOfWeek()
            );
            if (dueDateIntervalData.isDueDateMustBeWorkingDay() && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    dueDateIntervalData.getDueDateNonWorkingCalendar(),
                    dueDateIntervalData.getDueDateNonWorkingDaysOfWeek()
                );
            }
        }

        if (StringUtils.isNotBlank(dueDateIntervalData.getDueDateTime())) {
            return localDate.atTime(LocalTime.parse(dueDateIntervalData.getDueDateTime()));
        } else {
            return localDate.atTime(dueDate.toLocalTime());
        }
    }

    private DueDateIntervalData readDueDateOriginFields(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        return DueDateIntervalData.builder()
            .dueDateOrigin(dueDateProperties.stream()
                               .filter(r -> r.getName().getValue().equals(
                                   DUE_DATE_ORIGIN))
                               .reduce((a, b) -> b)
                               .map(ConfigurationDmnEvaluationResponse::getValue)
                               .map(CamundaValue::getValue)
                               .orElse(DEFAULT_ZONED_DATE_TIME.format(DUE_DATE_TIME_FORMATTER)))
            .dueDateIntervalDays(dueDateProperties.stream()
                                     .filter(r -> r.getName().getValue().equals(DUE_DATE_INTERVAL_DAYS))
                                     .reduce((a, b) -> b)
                                     .map(ConfigurationDmnEvaluationResponse::getValue)
                                     .map(CamundaValue::getValue)
                                     .map(Long::valueOf)
                                     .orElse(0L))
            .dueDateNonWorkingCalendar(dueDateProperties.stream()
                                           .filter(r -> r.getName().getValue().equals(DUE_DATE_NON_WORKING_CALENDAR))
                                           .reduce((a, b) -> b)
                                           .map(ConfigurationDmnEvaluationResponse::getValue)
                                           .map(CamundaValue::getValue)
                                           .orElse(DEFAULT_NON_WORKING_CALENDAR))
            .dueDateNonWorkingDaysOfWeek(dueDateProperties.stream()
                                             .filter(r -> r.getName().getValue().equals(
                                                 DUE_DATE_NON_WORKING_DAYS_OF_WEEK))
                                             .reduce((a, b) -> b)
                                             .map(ConfigurationDmnEvaluationResponse::getValue)
                                             .map(CamundaValue::getValue)
                                             .map(s -> s.split(","))
                                             .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                             .map(Arrays::asList)
                                             .orElse(List.of()))
            .dueDateSkipNonWorkingDays(dueDateProperties.stream()
                                           .filter(r -> r.getName().getValue()
                                               .equals(DUE_DATE_SKIP_NON_WORKING_DAYS))
                                           .reduce((a, b) -> b)
                                           .map(ConfigurationDmnEvaluationResponse::getValue)
                                           .map(CamundaValue::getValue)
                                           .map(Boolean::parseBoolean)
                                           .orElse(false))
            .dueDateMustBeWorkingDay(dueDateProperties.stream()
                                         .filter(r -> r.getName().getValue().equals(DUE_DATE_MUST_BE_WORKING_DAYS))
                                         .reduce((a, b) -> b)
                                         .map(ConfigurationDmnEvaluationResponse::getValue)
                                         .map(CamundaValue::getValue)
                                         .map(Boolean::parseBoolean)
                                         .orElse(false))
            .dueDateTime(dueDateProperties.stream()
                             .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                             .reduce((a, b) -> b)
                             .map(ConfigurationDmnEvaluationResponse::getValue)
                             .map(CamundaValue::getValue)
                             .orElse(DEFAULT_DUE_DATE_TIME))
            .build();
    }
}
