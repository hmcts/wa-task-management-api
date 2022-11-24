package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DueDateTimeReCalculator implements DateCalculator {

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties, boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse dueDateTime = getProperty(dueDateProperties, DUE_DATE_TIME);
        return Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN)).isEmpty()
            && Optional.ofNullable(dueDateTime).isPresent()
            && dueDateTime.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE
            && isReconfigureRequest;
    }

    @Override
    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);
        return addTimeToDate(dueDateTimeResponse, DEFAULT_DATE);
    }
}
