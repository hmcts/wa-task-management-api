package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateTimeCalculator extends DueDateTimeCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse intermediateOrigin = getProperty(
            dueDateProperties,
            dateTypeName + ORIGIN_SUFFIX,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse intermediateTime = getProperty(
            dueDateProperties,
            dateTypeName + TIME_SUFFIX,
            isReconfigureRequest
        );
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(intermediateOrigin).isEmpty()
            && Optional.ofNullable(intermediateTime).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest
    ) {
        return calculatedDate(
            dateTypeObject,
            getProperty(configResponses, dateTypeObject.dateTypeName() + TIME_SUFFIX, isReconfigureRequest)
        );
    }
}
