package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DueDateReCalculator implements DateCalculator {

    @Override
    public boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties, boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse property = getProperty(dueDateProperties, DUE_DATE);
        return Optional.ofNullable(property).isPresent()
            && property.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE
            && isReconfigureRequest;
    }

    @Override
    public LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties) {
        var dueDateResponse = getProperty(dueDateProperties, DUE_DATE);
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);

        if (Optional.ofNullable(dueDateTimeResponse).isPresent()) {
            return calculateDueDateFrom(dueDateResponse, dueDateTimeResponse);
        } else {
            return calculateDueDateFrom(dueDateResponse);
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
