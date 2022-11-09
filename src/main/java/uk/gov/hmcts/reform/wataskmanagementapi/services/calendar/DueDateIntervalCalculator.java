package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DueDateIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DueDateIntervalData.DUE_DATE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DueDateIntervalData.DUE_DATE_MUST_BE_WORKING_DAY_PREVIOUS;

@Slf4j
@Component
public class DueDateIntervalCalculator implements DateCalculator {
    private final WorkingDayIndicator workingDayIndicator;

    public DueDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        return Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN)).isPresent()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE)).isEmpty();
    }

    @Override
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

            if (dueDateIntervalData.getDueDateMustBeWorkingDay()
                .equalsIgnoreCase(DUE_DATE_MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    dueDateIntervalData.getDueDateNonWorkingCalendar(),
                    dueDateIntervalData.getDueDateNonWorkingDaysOfWeek()
                );
            }
            if (dueDateIntervalData.getDueDateMustBeWorkingDay()
                .equalsIgnoreCase(DUE_DATE_MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
                localDate = workingDayIndicator.getPreviousWorkingDay(
                    localDate,
                    dueDateIntervalData.getDueDateNonWorkingCalendar(),
                    dueDateIntervalData.getDueDateNonWorkingDaysOfWeek()
                );
            }
        }
        return localDate.atTime(LocalTime.parse(dueDateIntervalData.getDueDateTime()));
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
                                         .orElse("Next"))
            .dueDateTime(dueDateProperties.stream()
                             .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                             .reduce((a, b) -> b)
                             .map(ConfigurationDmnEvaluationResponse::getValue)
                             .map(CamundaValue::getValue)
                             .orElse(DEFAULT_DUE_DATE_TIME))
            .build();
    }
}
