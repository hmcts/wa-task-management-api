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
public class DueDateCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType())).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse>
                                                                    dueDateProperties, DateType dateType) {
        var dueDateResponse = getProperty(dueDateProperties, DUE_DATE.getType());
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter()
                                                .format(getDueDate(dueDateResponse, dueDateTimeResponse))))
            .build();
    }

    private LocalDateTime getDueDate(ConfigurationDmnEvaluationResponse dueDateResponse,
                                     ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
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
