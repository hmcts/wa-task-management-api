package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DueDateCalculator implements DateCalculator {

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        return getProperty(dueDateProperties, DUE_DATE).isPresent();
    }

    @Override
    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        var dueDateResponse = getProperty(dueDateProperties, DUE_DATE);
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);

        if (dueDateTimeResponse.isPresent()) {
            return calculateDueDateFrom(dueDateResponse.get(), dueDateTimeResponse.get());
        } else {
            return calculateDueDateFrom(dueDateResponse.get());
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        LocalDateTime parsedDueDate = parseDueDateTime(dueDate);
        if (parsedDueDate.getHour() == 0) {
            return parsedDueDate.withHour(16).withMinute(0);
        } else {
            return parsedDueDate;
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse,
                                               ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        return addTimeToDate(dueDateTimeResponse, parseDueDateTime(dueDate));
    }
}
