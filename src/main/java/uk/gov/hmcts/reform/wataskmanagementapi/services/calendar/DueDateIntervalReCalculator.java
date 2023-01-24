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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateIntervalReCalculator extends DueDateIntervalCalculator {

    public DueDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        var dueDateOrigin = getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN);
        var dueDate = getReConfigurableProperty(dueDateProperties, DUE_DATE.getType());
        return DUE_DATE == dateType
            && Optional.ofNullable(dueDateOrigin).isPresent()
            && Optional.ofNullable(dueDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        return calculateDate(dateType, readDueDateOriginFields(dueDateProperties));
    }

    private DateTypeIntervalData readDueDateOriginFields(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        return DateTypeIntervalData.builder()
            .dateTypeOrigin(dueDateProperties.stream()
                                .filter(r -> r.getName().getValue().equals(DUE_DATE_ORIGIN))
                                .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                .reduce((a, b) -> b)
                                .map(ConfigurationDmnEvaluationResponse::getValue)
                                .map(CamundaValue::getValue)
                                .orElse(DEFAULT_ZONED_DATE_TIME.format(DATE_TIME_FORMATTER)))
            .dateTypeIntervalDays(dueDateProperties.stream()
                                      .filter(r -> r.getName().getValue().equals(DUE_DATE_INTERVAL_DAYS))
                                      .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                      .reduce((a, b) -> b)
                                      .map(ConfigurationDmnEvaluationResponse::getValue)
                                      .map(CamundaValue::getValue)
                                      .map(Long::valueOf)
                                      .orElse(0L))
            .dateTypeNonWorkingCalendar(dueDateProperties.stream()
                                            .filter(r -> r.getName().getValue().equals(DUE_DATE_NON_WORKING_CALENDAR))
                                            .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
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
                                              .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
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
                                            .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                            .reduce((a, b) -> b)
                                            .map(ConfigurationDmnEvaluationResponse::getValue)
                                            .map(CamundaValue::getValue)
                                            .map(Boolean::parseBoolean)
                                            .orElse(false))
            .dateTypeMustBeWorkingDay(dueDateProperties.stream()
                                          .filter(r -> r.getName().getValue().equals(DUE_DATE_MUST_BE_WORKING_DAYS))
                                          .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                                          .reduce((a, b) -> b)
                                          .map(ConfigurationDmnEvaluationResponse::getValue)
                                          .map(CamundaValue::getValue)
                                          .orElse(DATE_TYPE_MUST_BE_WORKING_DAY_NEXT))
            .dateTypeTime(dueDateProperties.stream()
                              .filter(r -> r.getName().getValue().equals(DUE_DATE_TIME))
                              .filter(r -> r.getCanReconfigure().getValue().booleanValue() == TRUE)
                              .reduce((a, b) -> b)
                              .map(ConfigurationDmnEvaluationResponse::getValue)
                              .map(CamundaValue::getValue)
                              .orElse(DEFAULT_DATE_TIME))
            .build();
    }
}
