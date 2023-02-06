package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateIntervalCalculator implements DateCalculator {
    final WorkingDayIndicator workingDayIndicator;

    public DueDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN)).isPresent()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType())).isEmpty()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateType dateType, List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculateDate(dateType, readDateTypeOriginFields(configResponses, false));
    }

    protected ConfigurationDmnEvaluationResponse calculateDate(
        DateType dateType, DateTypeIntervalData dateTypeIntervalData) {

        LocalDate referenceDate = getReferenceDateForCalculation(dateTypeIntervalData).toLocalDate();
        if (dateTypeIntervalData.isDateTypeSkipNonWorkingDays()) {

            for (int counter = 0; counter < dateTypeIntervalData.getDateTypeIntervalDays(); counter++) {
                referenceDate = workingDayIndicator.getNextWorkingDay(
                    referenceDate,
                    dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                    dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
                );
            }
        } else {

            referenceDate = referenceDate.plusDays(dateTypeIntervalData.getDateTypeIntervalDays());
            boolean workingDay = workingDayIndicator.isWorkingDay(
                referenceDate,
                dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
            );
            if (dateTypeIntervalData.getDateTypeMustBeWorkingDay()
                .equalsIgnoreCase(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
                referenceDate = workingDayIndicator.getNextWorkingDay(
                    referenceDate,
                    dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                    dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
                );
            }
            if (dateTypeIntervalData.getDateTypeMustBeWorkingDay()
                .equalsIgnoreCase(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
                referenceDate = workingDayIndicator.getPreviousWorkingDay(
                    referenceDate,
                    dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                    dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
                );
            }
        }

        LocalDateTime dateTime = referenceDate.atTime(LocalTime.parse(dateTypeIntervalData.getDateTypeTime()));

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private static LocalDateTime getReferenceDateForCalculation(DateTypeIntervalData dateTypeIntervalData) {
        LocalDateTime calculatedRefDate = dateTypeIntervalData.getCalculatedRefDate();
        LocalDateTime calculatedEarliestDate = dateTypeIntervalData.getCalculatedEarliestDate();
        if (Optional.ofNullable(calculatedRefDate).isPresent()) {
            return calculatedRefDate;
        } else if (Optional.ofNullable(calculatedEarliestDate).isPresent()) {
            return calculatedEarliestDate;
        } else {
            return LocalDateTime.parse(dateTypeIntervalData.getDateTypeOrigin(), DATE_TIME_FORMATTER);
        }
    }

    protected DateTypeIntervalData readDateTypeOriginFields(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, boolean reconfigure) {
        return DateTypeIntervalData.builder()
            .dateTypeOrigin(dueDateProperties.stream()
                                .filter(r -> r.getName().getValue().equals(DUE_DATE_ORIGIN))
                                .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .dateTypeIntervalDays(dueDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(DUE_DATE_INTERVAL_DAYS))
                                      .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(dueDateProperties.stream()
                                            .filter(r -> r.getName().getValue().equals(DUE_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(s -> s.split(","))
                                            .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                            .map(Arrays::asList)
                                            .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .dateTypeNonWorkingDaysOfWeek(dueDateProperties.stream()
                                              .filter(r -> r.getName().getValue().equals(
                                                  DUE_DATE_NON_WORKING_DAYS_OF_WEEK))
                                              .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .map(s -> s.split(","))
                                              .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                              .map(Arrays::asList)
                                              .orElse(List.of()))
            .dateTypeSkipNonWorkingDays(dueDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(DUE_DATE_SKIP_NON_WORKING_DAYS))
                                            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(dueDateProperties.stream()
                                          .filter(r -> r.getName().getValue().equals(DUE_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(dueDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                              .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
