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
        DateType dateType,
        boolean isReconfigureRequest) {
        return DUE_DATE == dateType
            && Optional.ofNullable(getReConfigurableProperty(dueDateProperties, DUE_DATE.getType())).isPresent()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType) {
        var dueDateResponse = getReConfigurableProperty(dueDateProperties, DUE_DATE.getType());
        var dueDateTimeResponse = getReConfigurableProperty(dueDateProperties, DUE_DATE_TIME);
        return calculatedDate(dateType, dueDateResponse, dueDateTimeResponse);
    }
}
