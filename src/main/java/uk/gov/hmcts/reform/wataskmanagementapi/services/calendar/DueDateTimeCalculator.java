package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateTimeCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_TIME, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return calculatedDate(dateTypeObject, getProperty(configResponses, DUE_DATE_TIME, isReconfigureRequest));
    }

    protected ConfigurationDmnEvaluationResponse calculatedDate(
        DateTypeObject dateTypeObject,
        ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        LocalDateTime dateTime = addTimeToDate(dueDateTimeResponse, DEFAULT_DATE);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateTypeObject.dateTypeName()))
            .value(CamundaValue.stringValue(dateTypeObject.dateType().getDateTimeFormatter().format(dateTime)))
            .build();
    }
}
