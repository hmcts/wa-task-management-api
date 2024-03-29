package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN_REF, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN_EARLIEST, isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN_LATEST, isReconfigureRequest))
            .isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_TIME, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        var dueDateTimeResponse = getProperty(configResponses, DUE_DATE_TIME, isReconfigureRequest);
        log.info("Input {}: {}", DUE_DATE_TIME, dueDateTimeResponse);
        return calculatedDate(dateTypeObject, dueDateTimeResponse, isReconfigureRequest);
    }

    protected ConfigurationDmnEvaluationResponse calculatedDate(
        DateTypeObject dateTypeObject,
        ConfigurationDmnEvaluationResponse dueDateTimeResponse, boolean isReconfigureRequest) {
        LocalDateTime dateTime = addTimeToDate(dueDateTimeResponse, DEFAULT_DATE);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateTypeObject.dateTypeName()))
            .value(CamundaValue.stringValue(dateTypeObject.dateType().getDateTimeFormatter().format(dateTime)))
            .canReconfigure(CamundaValue.booleanValue(isReconfigureRequest))
            .build();
    }
}
