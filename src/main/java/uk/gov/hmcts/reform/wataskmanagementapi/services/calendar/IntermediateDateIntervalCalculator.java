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
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.domain.calendar.DateTypeIntervalData.DATE_TYPE_MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateIntervalCalculator extends DueDateIntervalCalculator {

    public IntermediateDateIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse intermediateOrigin = getProperty(
            configResponses,
            dateTypeName + ORIGIN_SUFFIX,
            //always intermediate date values will be read hence isReconfigurableRequest value is set to false
            false
        );
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(intermediateOrigin).isPresent()
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, dateTypeName);
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        Optional<LocalDateTime> referenceDate = getReferenceDate(
            dateTypeObject.dateTypeName(),
            configResponses,
            isReconfigureRequest,
            taskAttributes,
            calculatedConfigurations
        );
        return referenceDate.map(localDateTime -> calculateDate(
                dateTypeObject,
                readDateTypeOriginFields(dateTypeObject.dateTypeName(), configResponses, isReconfigureRequest),
                localDateTime,
                isReconfigureRequest))
            .orElse(addEmptyConfiguration(dateTypeObject.dateTypeName()));
    }

    protected Optional<LocalDateTime> getReferenceDate(
        String dateTypeName,
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        boolean reconfigure,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dateTypeName + ORIGIN_SUFFIX))
            .reduce((a, b) -> b)
            .map(v -> {
                log.info("Input {}: {}", dateTypeName + ORIGIN_SUFFIX, v);
                return v.getValue().getValue();
            })
            .map(this::parseDateTime);
    }

    private  ConfigurationDmnEvaluationResponse addEmptyConfiguration(String type) {
        return ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue(type))
            .value(CamundaValue.stringValue(""))
            .build();
    }

    protected DateTypeIntervalData readDateTypeOriginFields(
        String dateTypeName,
        List<ConfigurationDmnEvaluationResponse> nextHearingDateProperties,
        boolean reconfigure) {

        return DateTypeIntervalData.builder()
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
