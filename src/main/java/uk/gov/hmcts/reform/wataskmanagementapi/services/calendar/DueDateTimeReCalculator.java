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
public class DueDateTimeReCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest
    ) {
        ConfigurationDmnEvaluationResponse dueDateTime = getProperty(dueDateProperties, DUE_DATE_TIME);
        ConfigurationDmnEvaluationResponse dueDate = getProperty(dueDateProperties, DUE_DATE.getType());
        ConfigurationDmnEvaluationResponse dueDateOrigin = getProperty(dueDateProperties, DUE_DATE_ORIGIN);
        return DUE_DATE == dateType
            && isReconfigureRequest
            && (Optional.ofNullable(dueDate).isEmpty()
            || dueDate.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && (Optional.ofNullable(dueDateOrigin).isEmpty()
            || dueDateOrigin.getCanReconfigure().getValue().booleanValue() == Boolean.FALSE)
            && Optional.ofNullable(dueDateTime).isPresent()
            && dueDateTime.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        var dueDateTimeResponse = getProperty(dueDateProperties, DUE_DATE_TIME);
        LocalDateTime dateTime = addTimeToDate(dueDateTimeResponse, DEFAULT_DATE);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }
}
