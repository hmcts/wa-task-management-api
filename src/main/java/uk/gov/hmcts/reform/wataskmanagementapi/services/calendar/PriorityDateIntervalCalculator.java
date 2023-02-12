package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateIntervalCalculator extends DueDateIntervalCalculator {

    public PriorityDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return PRIORITY_DATE == dateType
            && Optional.ofNullable(getProperty(priorityDateProperties, PRIORITY_DATE_ORIGIN,
                                               isReconfigureRequest)).isPresent()
            && Optional.ofNullable(getProperty(priorityDateProperties, PRIORITY_DATE.getType(),
                                               isReconfigureRequest)).isEmpty();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
            List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
            DateType dateType, boolean isReconfigureRequest) {
        return calculateDate(dateType, readDateTypeOriginFields(priorityDateProperties, false));
    }

    @Override
    protected DateTypeIntervalData readDateTypeOriginFields(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties, boolean reconfigure) {

        return DateTypeIntervalData.builder()
            .dateTypeOrigin(priorityDateProperties.stream()
                                .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_ORIGIN))
                                .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .dateTypeIntervalDays(priorityDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_INTERVAL_DAYS))
                                      .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(priorityDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(PRIORITY_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(s -> s.split(","))
                                            .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                            .map(Arrays::asList)
                                            .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .dateTypeNonWorkingDaysOfWeek(priorityDateProperties.stream()
                                              .filter(r -> r.getName().getValue()
                                                  .equals(PRIORITY_DATE_NON_WORKING_DAYS_OF_WEEK))
                                              .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                              .reduce((a, b) -> b)
                                              .map(ConfigurationDmnEvaluationResponse::getValue)
                                              .map(CamundaValue::getValue)
                                              .map(s -> s.split(","))
                                              .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                              .map(Arrays::asList)
                                              .orElse(List.of()))
            .dateTypeSkipNonWorkingDays(priorityDateProperties.stream()
                                            .filter(r -> r.getName().getValue()
                                                .equals(PRIORITY_DATE_SKIP_NON_WORKING_DAYS))
                                            .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(priorityDateProperties.stream()
                                          .filter(r -> r.getName().getValue()
                                              .equals(PRIORITY_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(priorityDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(PRIORITY_DATE_TIME))
                              .filter(r -> !reconfigure  || r.getCanReconfigure().getValue())
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(null))
            .build();
    }
}