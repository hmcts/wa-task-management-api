package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateReCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse dueDate = getProperty(dueDateProperties, DUE_DATE.getType());
        return DUE_DATE == dateType
            && Optional.ofNullable(dueDate).isPresent()
            && dueDate.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        var dueDateResponse = getProperty(dueDateProperties, DUE_DATE.getType());
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);

        LocalDateTime dateTime = getDateTime(dueDateResponse, dueDateTimeResponse);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private LocalDateTime getDateTime(ConfigurationDmnEvaluationResponse dueDateResponse,
                                      ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        if (Optional.ofNullable(dueDateTimeResponse).isEmpty()
            || dueDateTimeResponse.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE) {
            return calculateDueDateFrom(dueDateResponse);
        } else {
            return calculateDueDateFrom(dueDateResponse, dueDateTimeResponse);
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        LocalDateTime parsedDueDate = parseDateTime(dueDate);
        if (parsedDueDate.getHour() == 0) {
            return parsedDueDate.withHour(16).withMinute(0);
        } else {
            return parsedDueDate;
        }
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse,
                                               ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        return addTimeToDate(dueDateTimeResponse, parseDateTime(dueDate));
    }
}
