package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateTimeCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        String dateTypeName = dateTypeObject.dateTypeName();
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName + ORIGIN_SUFFIX)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName + TIME_SUFFIX)).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeObject dateTypeObject, List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculatedDate(
            dateTypeObject,
            getProperty(configResponses, dateTypeObject.dateTypeName() + TIME_SUFFIX)
        );
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
