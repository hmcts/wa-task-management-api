package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DueDateTimeCalculator implements DateCalculator {

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        return getProperty(dueDateProperties, DUE_DATE).isEmpty()
            && getProperty(dueDateProperties, DUE_DATE_ORIGIN).isEmpty()
            && getProperty(dueDateProperties, DUE_DATE_TIME).isPresent();
    }

    @Override
    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);
        return addTimeToDate(dueDateTimeResponse.get(), DEFAULT_DATE);
    }
}
