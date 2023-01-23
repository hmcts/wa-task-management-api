package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.PriorityDateIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.PriorityDateIntervalData.PRIORITY_DATE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.PriorityDateIntervalData.PRIORITY_DATE_MUST_BE_WORKING_DAY_PREVIOUS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateIntervalReCalculator implements DateCalculator {
    private final WorkingDayIndicator workingDayIndicator;

    public PriorityDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse priorityDateOrigin
            = getProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN);
        ConfigurationDmnEvaluationResponse priorityDate = getProperty(priorityDateProperties, PRIORITY_DATE.getType());
        return PRIORITY_DATE == dateType
            && Optional.ofNullable(priorityDateOrigin).isPresent()
            && priorityDateOrigin.getCanReconfigure().getValue().booleanValue() == TRUE
            && (Optional.ofNullable(priorityDate).isEmpty()
            || priorityDate.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType) {
        PriorityDateIntervalData priorityDateIntervalData = readPriorityDateOriginFields(priorityDateProperties);

        LocalDateTime priorityDate = LocalDateTime.parse(
            priorityDateIntervalData.getPriorityDateOrigin(),
            DATE_TIME_FORMATTER
        );

        LocalDate localDate = priorityDate.toLocalDate();
        if (priorityDateIntervalData.isPriorityDateSkipNonWorkingDays()) {

            for (int counter = 0; counter < priorityDateIntervalData.getPriorityDateIntervalDays(); counter++) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    priorityDateIntervalData.getPriorityDateNonWorkingCalendar(),
                    priorityDateIntervalData.getPriorityDateNonWorkingDaysOfWeek()
                );
            }
        } else {

            localDate = localDate.plusDays(priorityDateIntervalData.getPriorityDateIntervalDays());
            boolean workingDay = workingDayIndicator.isWorkingDay(
                localDate,
                priorityDateIntervalData.getPriorityDateNonWorkingCalendar(),
                priorityDateIntervalData.getPriorityDateNonWorkingDaysOfWeek()
            );
            if (priorityDateIntervalData.getPriorityDateMustBeWorkingDay()
                .equalsIgnoreCase(PRIORITY_DATE_MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    priorityDateIntervalData.getPriorityDateNonWorkingCalendar(),
                    priorityDateIntervalData.getPriorityDateNonWorkingDaysOfWeek()
                );
            }
            if (priorityDateIntervalData.getPriorityDateMustBeWorkingDay()
                .equalsIgnoreCase(PRIORITY_DATE_MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
                localDate = workingDayIndicator.getPreviousWorkingDay(
                    localDate,
                    priorityDateIntervalData.getPriorityDateNonWorkingCalendar(),
                    priorityDateIntervalData.getPriorityDateNonWorkingDaysOfWeek()
                );
            }
        }
        LocalDateTime dateTime = localDate.atTime(LocalTime.parse(priorityDateIntervalData.getPriorityDateTime()));

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private PriorityDateIntervalData readPriorityDateOriginFields(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties) {
        return PriorityDateIntervalData.builder()
            .priorityDateOrigin(priorityDateProperties.stream()
                                    .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_ORIGIN))
                                    .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                    .reduce((a, b) -> b)
                                    .map(ConfigurationDmnEvaluationResponse::getValue)
                                    .map(CamundaValue::getValue)
                                    .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .priorityDateIntervalDays(priorityDateProperties.stream()
                                          .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_INTERVAL_DAYS))
                                          .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .map(Long::valueOf)
                                          .orElse(0L))
            .priorityDateNonWorkingCalendar(priorityDateProperties.stream()
                                                .filter(r -> r.getName().getValue()
                                                    .equals(PRIORITY_DATE_NON_WORKING_CALENDAR))
                                                .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                                .reduce((a, b) -> b)
                                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                                .map(CamundaValue::getValue)
                                                .map(s -> s.split(","))
                                                .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                                .map(Arrays::asList)
                                                .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .priorityDateNonWorkingDaysOfWeek(priorityDateProperties.stream()
                                                  .filter(r -> r.getName().getValue().equals(
                                                      PRIORITY_DATE_NON_WORKING_DAYS_OF_WEEK))
                                                  .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                                  .reduce((a, b) -> b)
                                                  .map(ConfigurationDmnEvaluationResponse::getValue)
                                                  .map(CamundaValue::getValue)
                                                  .map(s -> s.split(","))
                                                  .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                                  .map(Arrays::asList)
                                                  .orElse(List.of()))
            .priorityDateSkipNonWorkingDays(priorityDateProperties.stream()
                                                .filter(r -> r.getName().getValue()
                                                    .equals(PRIORITY_DATE_SKIP_NON_WORKING_DAYS))
                                                .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                                .reduce((a, b) -> b)
                                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                                .map(CamundaValue::getValue)
                                                .map(Boolean::parseBoolean)
                                                .orElse(false))
            .priorityDateMustBeWorkingDay(priorityDateProperties.stream()
                                              .filter(r -> r.getName().getValue()
                                                  .equals(PRIORITY_DATE_MUST_BE_WORKING_DAYS))
                                              .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .orElse(PRIORITY_DATE_MUST_BE_WORKING_DAY_NEXT))
            .priorityDateTime(priorityDateProperties.stream()
                                  .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_TIME))
                                  .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                  .reduce((a, b) -> b)
                                  .map(ConfigurationDmnEvaluationResponse::getValue)
                                  .map(CamundaValue::getValue)
                                  .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
