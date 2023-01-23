package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.NEXT_HEARING_DATE;

@Slf4j
@Component
public class NextHearingDateReCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        ConfigurationDmnEvaluationResponse dueDate = getProperty(dueDateProperties, NEXT_HEARING_DATE.getType());
        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(dueDate).isPresent()
            && dueDate.getCanReconfigure().getValue().booleanValue() == Boolean.TRUE
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        var dueDateResponse = getProperty(dueDateProperties, NEXT_HEARING_DATE.getType());
        var dueDateTimeResponse = getProperty(dueDateProperties, NEXT_HEARING_DATE_TIME);

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
            return calculateNextHearingDateFrom(dueDateResponse);
        } else {
            return calculateNextHearingDateFrom(dueDateResponse, dueDateTimeResponse);
        }
    }

    private LocalDateTime calculateNextHearingDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        LocalDateTime parsedNextHearingDate = parseDateTime(dueDate);
        if (parsedNextHearingDate.getHour() == 0) {
            return parsedNextHearingDate.withHour(16).withMinute(0);
        } else {
            return parsedNextHearingDate;
        }
    }

    private LocalDateTime calculateNextHearingDateFrom(ConfigurationDmnEvaluationResponse dueDateResponse,
                                               ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        String dueDate = dueDateResponse.getValue().getValue();
        return addTimeToDate(dueDateTimeResponse, parseDateTime(dueDate));
    }
}
