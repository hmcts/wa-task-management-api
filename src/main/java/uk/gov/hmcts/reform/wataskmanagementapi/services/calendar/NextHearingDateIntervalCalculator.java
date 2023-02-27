package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
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
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(nextHearingDateProperties, NEXT_HEARING_DATE_ORIGIN,
                                               isReconfigureRequest
        )).isPresent()
            && Optional.ofNullable(getProperty(nextHearingDateProperties, NEXT_HEARING_DATE.getType(),
                                               isReconfigureRequest
        )).isEmpty();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateType, boolean isReconfigureRequest) {

        Optional<LocalDateTime> referenceDate = getReferenceDate(configResponses, isReconfigureRequest);
        return referenceDate.map(localDateTime -> calculateDate(
            dateType,
            readDateTypeOriginFields(configResponses, isReconfigureRequest),
            localDateTime
        )).orElse(null);
    }

    @Override
    protected DateTypeIntervalData readDateTypeOriginFields(
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties, boolean reconfigure) {

        return DateTypeIntervalData.builder()
            .dateTypeIntervalDays(nextHearingDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_INTERVAL_DAYS))
                                      .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(nextHearingDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(NEXT_HEARING_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
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
                                              .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
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
                                            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(nextHearingDateProperties.stream()
                                          .filter(r -> r.getName().getValue()
                                              .equals(NEXT_HEARING_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(nextHearingDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_TIME))
                              .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(null))
            .build();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
        List<ConfigurationDmnEvaluationResponse> configResponses, boolean reconfigure) {
        return configResponses.stream()
            .filter(r -> r.getName().getValue().equals(NEXT_HEARING_DATE_ORIGIN))
            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
            .reduce((a, b) -> b)
            .map(ConfigurationDmnEvaluationResponse::getValue)
            .map(CamundaValue::getValue)
            .map(v -> LocalDateTime.parse(v, DATE_TIME_FORMATTER));
    }
}
