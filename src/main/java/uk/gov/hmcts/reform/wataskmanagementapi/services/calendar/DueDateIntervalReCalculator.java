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

import static java.lang.Boolean.TRUE;

@Slf4j
@Component
public class DueDateIntervalReCalculator implements DateCalculator {
    private final WorkingDayIndicator workingDayIndicator;

    public DueDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties, boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse dueDateOrigin = getProperty(dueDateProperties, DUE_DATE_ORIGIN);
        ConfigurationDmnEvaluationResponse dueDate = getProperty(dueDateProperties, DUE_DATE);
        return Optional.ofNullable(dueDateOrigin).isPresent()
            && dueDateOrigin.getCanReconfigure().getValue().booleanValue() == TRUE
            && (Optional.ofNullable(dueDate).isEmpty()
            || dueDate.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && isReconfigureRequest;
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
            if (dueDateIntervalData.isDueDateMustBeWorkingDay() && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
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
                               .filter(r -> r.getName().getValue().equals(DUE_DATE_ORIGIN))
                               .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                               .reduce((a, b) -> b)
                               .map(ConfigurationDmnEvaluationResponse::getValue)
                               .map(CamundaValue::getValue)
                               .orElse(DEFAULT_ZONED_DATE_TIME.format(DUE_DATE_TIME_FORMATTER)))
            .dueDateIntervalDays(dueDateProperties.stream()
                                     .filter(r -> r.getName().getValue().equals(DUE_DATE_INTERVAL_DAYS))
                                     .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                     .reduce((a, b) -> b)
                                     .map(ConfigurationDmnEvaluationResponse::getValue)
                                     .map(CamundaValue::getValue)
                                     .map(Long::valueOf)
                                     .orElse(0L))
            .dueDateNonWorkingCalendar(dueDateProperties.stream()
                                           .filter(r -> r.getName().getValue().equals(DUE_DATE_NON_WORKING_CALENDAR))
                                           .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                           .reduce((a, b) -> b)
                                           .map(ConfigurationDmnEvaluationResponse::getValue)
                                           .map(CamundaValue::getValue)
                                           .map(s -> s.split(","))
                                           .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                           .map(Arrays::asList)
                                           .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .dueDateNonWorkingDaysOfWeek(dueDateProperties.stream()
                                             .filter(r -> r.getName().getValue().equals(
                                                 DUE_DATE_NON_WORKING_DAYS_OF_WEEK))
                                             .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
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
                                           .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                           .reduce((a, b) -> b)
                                           .map(ConfigurationDmnEvaluationResponse::getValue)
                                           .map(CamundaValue::getValue)
                                           .map(Boolean::parseBoolean)
                                           .orElse(false))
            .dueDateMustBeWorkingDay(dueDateProperties.stream()
                                         .filter(r -> r.getName().getValue().equals(DUE_DATE_MUST_BE_WORKING_DAYS))
                                         .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                         .reduce((a, b) -> b)
                                         .map(ConfigurationDmnEvaluationResponse::getValue)
                                         .map(CamundaValue::getValue)
                                         .map(Boolean::parseBoolean)
                                         .orElse(false))
            .dueDateTime(dueDateProperties.stream()
                             .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                             .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                             .reduce((a, b) -> b)
                             .map(ConfigurationDmnEvaluationResponse::getValue)
                             .map(CamundaValue::getValue)
                             .orElse(DEFAULT_DUE_DATE_TIME))
            .build();
    }
}
