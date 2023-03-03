package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateTimeCalculator extends DueDateTimeCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        ConfigurationDmnEvaluationResponse priorityDateTime = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_TIME,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse prioritDateOrigin = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_ORIGIN,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse prioritDateOriginRef = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_ORIGIN_REF,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse prioritDateOriginLatest = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_ORIGIN_LATEST,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse prioritDateOriginEarliest = getProperty(
            priorityDateProperties,
            PRIORITY_DATE_ORIGIN_EARLIEST,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse priorityDate = getProperty(
            priorityDateProperties,
            PRIORITY_DATE.getType(),
            isReconfigureRequest
        );
        return PRIORITY_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(priorityDate).isEmpty()
            && Optional.ofNullable(prioritDateOrigin).isEmpty()
            && Optional.ofNullable(prioritDateOriginRef).isEmpty()
            && Optional.ofNullable(prioritDateOriginLatest).isEmpty()
            && Optional.ofNullable(prioritDateOriginEarliest).isEmpty()
            && Optional.ofNullable(priorityDateTime).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> priorityDateProperties,
        DateTypeObject dateType,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes, List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        return calculatedDate(dateType, getProperty(priorityDateProperties, PRIORITY_DATE_TIME, isReconfigureRequest));
    }
}
