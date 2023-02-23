package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {

        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeObject.dateTypeName(), isReconfigureRequest))
            .isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest
    ) {
        String dateTypeName = dateTypeObject.dateTypeName();
        return calculatedDate(
            dateTypeObject,
            getProperty(configResponses, dateTypeName, isReconfigureRequest),
            getProperty(configResponses, dateTypeName + TIME_SUFFIX, isReconfigureRequest)
        );
    }
}
