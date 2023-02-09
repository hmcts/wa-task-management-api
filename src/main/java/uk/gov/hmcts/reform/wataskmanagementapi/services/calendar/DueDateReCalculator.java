package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateReCalculator extends DueDateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        return DUE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, DUE_DATE.getType())).isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeConfigurator.DateTypeObject dateType, List<ConfigurationDmnEvaluationResponse> configResponses) {
        return calculatedDate(
            dateType,
            getReConfigurableProperty(configResponses, DUE_DATE.getType()),
            getReConfigurableProperty(configResponses, DUE_DATE_TIME)
        );
    }
}
