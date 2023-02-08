package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateIntervalCalculator extends DueDateIntervalCalculator {

    public NextHearingDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(
                getProperty(nextHearingDateProperties, NEXT_HEARING_DATE_ORIGIN, isReconfigureRequest)).isPresent()
            && Optional.ofNullable(
                getProperty(nextHearingDateProperties, NEXT_HEARING_DATE.getType(), isReconfigureRequest)).isEmpty();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties, DateType dateType,
        boolean isReconfigureRequest) {

        DateTypeIntervalData nextHearingDateIntervalData = readNextHearingDateOriginFields(nextHearingDateProperties,
            isReconfigureRequest);
        LocalDateTime originDate = readNextHearingDateOrigin(nextHearingDateProperties, isReconfigureRequest);

        LocalDateTime dateTime = calculate(nextHearingDateIntervalData, originDate);

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    protected LocalDateTime readNextHearingDateOrigin(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties, boolean reconfigure) {
        return nextHearingDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_ORIGIN))
            .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
            .reduce((a, b) -> b)
            .map(r -> LocalDateTime.parse(r.getValue().getValue(), DATE_TIME_FORMATTER))
            .orElse(DEFAULT_ZONED_DATE_TIME);
    }

    protected DateTypeIntervalData readNextHearingDateOriginFields(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties, boolean reconfigure) {

        return DateTypeIntervalData.builder()
            .dateTypeIntervalDays(nextHearingDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_INTERVAL_DAYS))
                                      .filter(r -> !reconfigure  || canReconfigure(r))
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(nextHearingDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(NEXT_HEARING_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> !reconfigure  || canReconfigure(r))
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(s -> s.split(","))
                                            .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                            .map(Arrays::asList)
                                            .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .dateTypeNonWorkingDaysOfWeek(nextHearingDateProperties.stream()
                                              .filter(r -> r.getName().getValue()
                                                  .equals(NEXT_HEARING_DATE_NON_WORKING_DAYS_OF_WEEK))
                                              .filter(r -> !reconfigure  || canReconfigure(r))
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .map(s -> s.split(","))
                                              .map(a -> Arrays.stream(a)
                                                  .map(String::trim).toArray(String[]::new))
                                              .map(Arrays::asList)
                                              .orElse(List.of()))
            .dateTypeSkipNonWorkingDays(nextHearingDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(NEXT_HEARING_DATE_SKIP_NON_WORKING_DAYS))
                                            .filter(r -> !reconfigure  || canReconfigure(r))
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(nextHearingDateProperties.stream()
                                          .filter(r -> r.getName().getValue()
                                              .equals(NEXT_HEARING_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> !reconfigure  || canReconfigure(r))
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(nextHearingDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_TIME))
                              .filter(r -> !reconfigure  || canReconfigure(r))
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
