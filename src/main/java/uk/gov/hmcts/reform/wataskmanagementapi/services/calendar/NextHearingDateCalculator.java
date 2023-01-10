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
public class NextHearingDateCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return NEXT_HEARING_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, NEXT_HEARING_DATE.getType())).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        var nextHearingDateResponse = getProperty(dueDateProperties, NEXT_HEARING_DATE.getType());
        LocalDateTime dateTime = calculateDueDateFrom(nextHearingDateResponse);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }

    private LocalDateTime calculateDueDateFrom(ConfigurationDmnEvaluationResponse nextHearingDateResponse) {
        String nextHearingDate = nextHearingDateResponse.getValue().getValue();
        LocalDateTime parsedNextHearingDate = parseDueDateTime(nextHearingDate);
        if (parsedNextHearingDate.getHour() == 0) {
            return parsedNextHearingDate.withHour(16).withMinute(0);
        } else {
            return parsedNextHearingDate;
        }
    }
}
