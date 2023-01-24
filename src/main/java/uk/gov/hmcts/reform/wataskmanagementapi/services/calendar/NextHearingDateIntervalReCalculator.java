package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.NextHearingDateIntervalData.NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateIntervalReCalculator extends DueDateIntervalCalculator {

    public NextHearingDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
                            DateType dateType,
                            boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse nextHearingDateOrigin = getReConfigurableProperty(
            nextHearingDateProperties,
            NEXT_HEARING_DATE_ORIGIN
        );
        ConfigurationDmnEvaluationResponse nextHearingDate = getReConfigurableProperty(
            nextHearingDateProperties,
            NEXT_HEARING_DATE.getType()
        );
        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(nextHearingDateOrigin).isPresent()
            && Optional.ofNullable(nextHearingDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        DateType dateType) {
        DateTypeIntervalData nextHearingDateIntervalData = readOriginFields(nextHearingDateProperties);

        return calculateDate(dateType, nextHearingDateIntervalData);
    }

    private DateTypeIntervalData readOriginFields(List<ConfigurationDmnEvaluationResponse> dateTypeProperties) {
        return DateTypeIntervalData.builder()
            .dateTypeOrigin(dateTypeProperties.stream()
                                .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_ORIGIN))
                                .filter(r -> r.getCanReconfigure().getValue())
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .dateTypeIntervalDays(dateTypeProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_INTERVAL_DAYS))
                                      .filter(r -> r.getCanReconfigure().getValue())
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(dateTypeProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(NEXT_HEARING_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(s -> s.split(","))
                                            .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                            .map(Arrays::asList)
                                            .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .dateTypeNonWorkingDaysOfWeek(dateTypeProperties.stream()
                                              .filter(r -> r.getName().getValue().equals(
                                                  NEXT_HEARING_DATE_NON_WORKING_DAYS_OF_WEEK))
                                              .filter(r -> r.getCanReconfigure().getValue())
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .map(s -> s.split(","))
                                              .map(a -> Arrays.stream(a).map(String::trim)
                                                  .toArray(String[]::new))
                                              .map(Arrays::asList)
                                              .orElse(List.of()))
            .dateTypeSkipNonWorkingDays(dateTypeProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(NEXT_HEARING_DATE_SKIP_NON_WORKING_DAYS))
                                            .filter(r -> r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(dateTypeProperties.stream()
                                          .filter(r -> r.getName().getValue().equals(
                                              NEXT_HEARING_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> r.getCanReconfigure().getValue())
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(NEXT_HEARING_DATE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(dateTypeProperties.stream()
                              .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_TIME))
                              .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
