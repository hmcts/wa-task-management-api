package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DueDateCalculator.DEFAULT_ZONED_DATE_TIME;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar.DueDateCalculator.DUE_DATE_TIME_FORMATTER;

@Slf4j
@Component
public class DueDateOriginBasedCalculator {
    public static final String DUE_DATE_ORIGIN = "dueDateOrigin";
    public static final String DUE_DATE_INTERVAL_DAYS = "dueDateIntervalDays";
    public static final String DUE_DATE_NON_WORKING_CALENDAR = "dueDateNonWorkingCalendar";
    public static final String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    public static final String DUE_DATE_NON_WORKING_DAYS_OF_WEEK = "dueDateNonWorkingDaysOfWeek";
    public static final String DUE_DATE_SKIP_NON_WORKING_DAYS = "dueDateSkipNonWorkingDays";
    public static final String DUE_DATE_MUST_BE_WORKING_DAYS = "dueDateMustBeWorkingDay";
    public static final String DUE_DATE_TIME = "dueDateTime";
    private final WorkingDayIndicator workingDayIndicator;

    public DueDateOriginBasedCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        DueDateOriginData dueDateOriginData = readDueDateOriginFields(dueDateProperties);

        LocalDateTime dueDate = LocalDateTime.parse(dueDateOriginData.dueDateOrigin, DUE_DATE_TIME_FORMATTER);

        LocalDate localDate = dueDate.toLocalDate();
        LocalTime localTime = dueDate.toLocalTime();
        if (dueDateOriginData.dueDateSkipNonWorkingDays) {

            for (int counter = 0; counter < dueDateOriginData.dueDateIntervalDays; counter++) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    dueDateOriginData.dueDateNonWorkingCalendar,
                    dueDateOriginData.dueDateNonWorkingDaysOfWeek
                );
            }
        } else {

            localDate = localDate.plusDays(dueDateOriginData.dueDateIntervalDays);
            boolean workingDay = workingDayIndicator.isWorkingDay(
                localDate,
                dueDateOriginData.dueDateNonWorkingCalendar,
                dueDateOriginData.dueDateNonWorkingDaysOfWeek
            );
            if (dueDateOriginData.dueDateMustBeWorkingDay && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    dueDateOriginData.dueDateNonWorkingCalendar,
                    dueDateOriginData.dueDateNonWorkingDaysOfWeek
                );
            }
        }

        if (StringUtils.isNotBlank(dueDateOriginData.dueDateTime)) {
            return localDate.atTime(LocalTime.parse(dueDateOriginData.dueDateTime));
        } else return localDate.atTime(localTime);
    }

    private DueDateOriginData readDueDateOriginFields(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        DueDateOriginData.DueDateOriginDataBuilder builder = DueDateOriginData.builder();
        builder.dueDateOrigin(dueDateProperties.stream()
                                  .filter(r -> r.getName().getValue().equals(DUE_DATE_ORIGIN))
                                  .reduce((a, b) -> b)
                                  .map(ConfigurationDmnEvaluationResponse::getValue)
                                  .map(CamundaValue::getValue)
                                  .orElse(DEFAULT_ZONED_DATE_TIME.format(DUE_DATE_TIME_FORMATTER)));

        builder.dueDateIntervalDays(dueDateProperties.stream()
                                        .filter(r -> r.getName().getValue().equals(DUE_DATE_INTERVAL_DAYS))
                                        .reduce((a, b) -> b)
                                        .map(ConfigurationDmnEvaluationResponse::getValue)
                                        .map(CamundaValue::getValue)
                                        .map(Long::valueOf)
                                        .orElse(0L));


        builder.dueDateNonWorkingCalendar(dueDateProperties.stream()
                                              .filter(r -> r.getName().getValue().equals(DUE_DATE_NON_WORKING_CALENDAR))
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .orElse(DEFAULT_NON_WORKING_CALENDAR));


        builder.dueDateNonWorkingDaysOfWeek(dueDateProperties.stream()
                                                .filter(r -> r.getName().getValue().equals(
                                                    DUE_DATE_NON_WORKING_DAYS_OF_WEEK))
                                                .reduce((a, b) -> b)
                                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                                .map(CamundaValue::getValue)
                                                .map(s -> s.split(","))
                                                .map(Arrays::asList)
                                                .orElse(List.of()));


        builder.dueDateSkipNonWorkingDays(dueDateProperties.stream()
                                              .filter(r -> r.getName().getValue().equals(DUE_DATE_SKIP_NON_WORKING_DAYS))
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .map(Boolean::parseBoolean)
                                              .orElse(false));

        builder.dueDateMustBeWorkingDay(dueDateProperties.stream()
                                            .filter(r -> r.getName().getValue().equals(DUE_DATE_MUST_BE_WORKING_DAYS))
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false));

        builder.dueDateTime(dueDateProperties.stream()
                                .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse("16:00"));

        return builder.build();
    }


    @Builder
    public static class DueDateOriginData {
        String dueDateOrigin;
        Long dueDateIntervalDays;
        String dueDateNonWorkingCalendar;
        List<String> dueDateNonWorkingDaysOfWeek;
        boolean dueDateSkipNonWorkingDays;
        boolean dueDateMustBeWorkingDay;
        String dueDateTime;
    }
}
