package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS;
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
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(configResponses, DUE_DATE_ORIGIN, isReconfigureRequest)).isPresent()
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, DUE_DATE.getType());
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        return calculateDate(
            dateType,
            readDateTypeOriginFields(configResponses, isReconfigureRequest),
            getReferenceDate(configResponses, isReconfigureRequest, taskAttributes, calculatedConfigurations)
                .orElse(DEFAULT_ZONED_DATE_TIME)
        );
    }

    protected ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateTypeObject, DateTypeIntervalData dateTypeIntervalData, LocalDateTime referenceDate) {

        LocalDate localDate = referenceDate.toLocalDate();
        if (dateTypeIntervalData.isDateTypeSkipNonWorkingDays()) {
            localDate = calculateDateForSkipNonWorkingDays(localDate, dateTypeIntervalData);
        } else {
            localDate = calculateDateForNoSkip(localDate, dateTypeIntervalData);
        }

        LocalDateTime dateTime = calculateTime(dateTypeIntervalData.getDateTypeTime(), referenceDate, localDate);

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateTypeObject.dateTypeName()))
            .value(CamundaValue.stringValue(dateTypeObject.dateType().getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private LocalDate calculateDateForSkipNonWorkingDays(LocalDate localDate,
                                                         DateTypeIntervalData dateTypeIntervalData) {
        LocalDate calculatedDate = localDate;
        if (dateTypeIntervalData.getDateTypeIntervalDays() < 0) {
            for (long counter = dateTypeIntervalData.getDateTypeIntervalDays(); counter < 0; counter++) {
                calculatedDate = workingDayIndicator.getPreviousWorkingDay(
                    calculatedDate,
                    dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                    dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
                );
            }
        } else {
            for (int counter = 0; counter < dateTypeIntervalData.getDateTypeIntervalDays(); counter++) {
                calculatedDate = workingDayIndicator.getNextWorkingDay(
                    calculatedDate,
                    dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                    dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
                );
            }
        }
        return calculatedDate;
    }

    private LocalDate calculateDateForNoSkip(LocalDate localDate,
                                             DateTypeIntervalData dateTypeIntervalData) {
        LocalDate calculatedDate = localDate.plusDays(dateTypeIntervalData.getDateTypeIntervalDays());
        boolean workingDay = workingDayIndicator.isWorkingDay(
            calculatedDate,
            dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
            dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
        );
        if (dateTypeIntervalData.getDateTypeMustBeWorkingDay()
                .equalsIgnoreCase(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
            return workingDayIndicator.getNextWorkingDay(
                calculatedDate,
                dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
            );
        } else if (dateTypeIntervalData.getDateTypeMustBeWorkingDay()
                .equalsIgnoreCase(DATE_TYPE_MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
            return workingDayIndicator.getPreviousWorkingDay(
                calculatedDate,
                dateTypeIntervalData.getDateTypeNonWorkingCalendar(),
                dateTypeIntervalData.getDateTypeNonWorkingDaysOfWeek()
            );
        }
        return calculatedDate;
    }

    protected Optional<LocalDateTime> getReferenceDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean reconfigure,
        Map<String, Object> taskAttributes, List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        return configResponses.stream()
            .filter(r -> r.getName().getValue().equals(DUE_DATE_ORIGIN))
            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
            .reduce((a, b) -> b)
            .map(v -> {
                log.info("Input {}: {}", DUE_DATE_ORIGIN, v);
                return v.getValue().getValue();
            })
            .map(this::parseDateTime);
    }

    private LocalDateTime calculateTime(
        String dateTypeTime, LocalDateTime referenceDate, LocalDate calculateDate) {
        LocalTime baseReferenceTime = referenceDate.toLocalTime();
        LocalDateTime dateTime = calculateDate.atTime(baseReferenceTime);

        if (Optional.ofNullable(dateTypeTime).isPresent()) {
            dateTime = calculateDate.atTime(LocalTime.parse(dateTypeTime));
        } else if (dateTime.getHour() == 0) {
            dateTime = calculateDate.atTime(LocalTime.parse(DEFAULT_DATE_TIME));
        }
        return dateTime;
    }

    protected DateTypeIntervalData readDateTypeOriginFields(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, boolean reconfigure) {
        return DateTypeIntervalData.builder()
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
                                            .orElse(true))
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
                              .orElse(null))
            .build();
    }
}
