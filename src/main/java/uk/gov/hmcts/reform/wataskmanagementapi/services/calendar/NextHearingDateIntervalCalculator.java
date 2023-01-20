package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.NextHearingDateIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.NextHearingDateIntervalData.NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.NextHearingDateIntervalData.NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_PREVIOUS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateIntervalCalculator implements DateCalculator {
    private final WorkingDayIndicator workingDayIndicator;

    public NextHearingDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(getProperty(nextHearingDateProperties, NEXT_HEARING_DATE_ORIGIN)).isPresent()
            && Optional.ofNullable(getProperty(nextHearingDateProperties, NEXT_HEARING_DATE.getType())).isEmpty()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateType dateType) {
        var nextHearingDateIntervalData = readNextHearingDateOriginFields(nextHearingDateProperties);

        LocalDateTime nextHearingDate = LocalDateTime.parse(
            nextHearingDateIntervalData.getNextHearingDateOrigin(),
            DATE_TIME_FORMATTER
        );

        LocalDate localDate = nextHearingDate.toLocalDate();
        if (nextHearingDateIntervalData.isNextHearingDateSkipNonWorkingDays()) {

            for (int counter = 0; counter < nextHearingDateIntervalData.getNextHearingDateIntervalDays(); counter++) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingCalendar(),
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingDaysOfWeek()
                );
            }
        } else {

            localDate = localDate.plusDays(nextHearingDateIntervalData.getNextHearingDateIntervalDays());
            boolean workingDay = workingDayIndicator.isWorkingDay(
                localDate,
                nextHearingDateIntervalData.getNextHearingDateNonWorkingCalendar(),
                nextHearingDateIntervalData.getNextHearingDateNonWorkingDaysOfWeek()
            );
            if (nextHearingDateIntervalData.getNextHearingDateMustBeWorkingDay()
                .equalsIgnoreCase(NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
                localDate = workingDayIndicator.getNextWorkingDay(
                    localDate,
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingCalendar(),
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingDaysOfWeek()
                );
            }
            if (nextHearingDateIntervalData.getNextHearingDateMustBeWorkingDay()
                .equalsIgnoreCase(NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
                localDate = workingDayIndicator.getPreviousWorkingDay(
                    localDate,
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingCalendar(),
                    nextHearingDateIntervalData.getNextHearingDateNonWorkingDaysOfWeek()
                );
            }
        }

        var dateTime = localDate.atTime(LocalTime.parse(nextHearingDateIntervalData.getNextHearingDateTime()));

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private NextHearingDateIntervalData readNextHearingDateOriginFields(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties) {

        return NextHearingDateIntervalData.builder()
            .nextHearingDateOrigin(nextHearingDateProperties.stream()
                                       .filter(r -> r.getName().getValue().equals(
                                           NEXT_HEARING_DATE_ORIGIN))
                                       .reduce((a, b) -> b)
                                       .map(ConfigurationDmnEvaluationResponse::getValue)
                                       .map(CamundaValue::getValue)
                                       .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .nextHearingDateIntervalDays(nextHearingDateProperties.stream()
                                             .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_INTERVAL_DAYS))
                                             .reduce((a, b) -> b)
                                             .map(ConfigurationDmnEvaluationResponse::getValue)
                                             .map(CamundaValue::getValue)
                                             .map(Long::valueOf)
                                             .orElse(0L))
            .nextHearingDateNonWorkingCalendar(nextHearingDateProperties.stream()
                                                   .filter(r -> r.getName().getValue()
                                                       .equals(NEXT_HEARING_DATE_NON_WORKING_CALENDAR))
                                                   .reduce((a, b) -> b)
                                                   .map(ConfigurationDmnEvaluationResponse::getValue)
                                                   .map(CamundaValue::getValue)
                                                   .map(s -> s.split(","))
                                                   .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                                   .map(Arrays::asList)
                                                   .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .nextHearingDateNonWorkingDaysOfWeek(nextHearingDateProperties.stream()
                                                     .filter(r -> r.getName().getValue()
                                                         .equals(NEXT_HEARING_DATE_NON_WORKING_DAYS_OF_WEEK))
                                                     .reduce((a, b) -> b)
                                                     .map(ConfigurationDmnEvaluationResponse::getValue)
                                                     .map(CamundaValue::getValue)
                                                     .map(s -> s.split(","))
                                                     .map(a -> Arrays.stream(a)
                                                         .map(String::trim).toArray(String[]::new))
                                                     .map(Arrays::asList)
                                                     .orElse(List.of()))
            .nextHearingDateSkipNonWorkingDays(nextHearingDateProperties.stream()
                                                   .filter(r -> r.getName().getValue()
                                                       .equals(NEXT_HEARING_DATE_SKIP_NON_WORKING_DAYS))
                                                   .reduce((a, b) -> b)
                                                   .map(ConfigurationDmnEvaluationResponse::getValue)
                                                   .map(CamundaValue::getValue)
                                                   .map(Boolean::parseBoolean)
                                                   .orElse(false))
            .nextHearingDateMustBeWorkingDay(nextHearingDateProperties.stream()
                                                 .filter(r -> r.getName().getValue()
                                                     .equals(NEXT_HEARING_DATE_MUST_BE_WORKING_DAYS))
                                                 .reduce((a, b) -> b)
                                                 .map(ConfigurationDmnEvaluationResponse::getValue)
                                                 .map(CamundaValue::getValue)
                                                 .orElse(NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT))
            .nextHearingDateTime(nextHearingDateProperties.stream()
                                     .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_TIME))
                                     .reduce((a, b) -> b)
                                     .map(ConfigurationDmnEvaluationResponse::getValue)
                                     .map(CamundaValue::getValue)
                                     .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
