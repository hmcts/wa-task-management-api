package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateIntervalCalculator extends DueDateIntervalCalculator {

    public IntermediateDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        String dateTypeName = dateTypeObject.dateTypeName();
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName + ORIGIN_SUFFIX)).isPresent()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName)).isEmpty()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateTypeObject, List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculateDate(dateTypeObject,
                             readDateTypeOriginFields(dateTypeObject.dateTypeName(), configResponses, false));
    }

    protected DateTypeIntervalData readDateTypeOriginFields(String dateTypeName,
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties, boolean reconfigure) {

        return DateTypeIntervalData.builder()
            .dateTypeOrigin(nextHearingDateProperties.stream()
                                .filter(r -> r.getName().getValue().equals(dateTypeName + ORIGIN_SUFFIX))
                                .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .dateTypeIntervalDays(nextHearingDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(dateTypeName + INTERVAL_DAYS_SUFFIX))
                                      .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(nextHearingDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(dateTypeName + NON_WORKING_CALENDAR_SUFFIX))
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
                                                  .equals(dateTypeName + NON_WORKING_DAYS_OF_WEEK_SUFFIX))
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
                                                .equals(dateTypeName + SKIP_NON_WORKING_DAYS_SUFFIX))
                                            .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(nextHearingDateProperties.stream()
                                          .filter(r -> r.getName().getValue()
                                              .equals(dateTypeName + MUST_BE_WORKING_DAY_SUFFIX))
                                          .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(nextHearingDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(dateTypeName + TIME_SUFFIX))
                              .filter(r -> !reconfigure || r.getCanReconfigure().getValue())
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(null))
            .build();
    }
}
