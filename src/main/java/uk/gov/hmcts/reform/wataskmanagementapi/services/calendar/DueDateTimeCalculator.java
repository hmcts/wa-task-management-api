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
public class DueDateTimeCalculator implements DateCalculator {

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {

        return DUE_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE.getType(), isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_ORIGIN, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, DUE_DATE_TIME, isReconfigureRequest)).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType, boolean isReconfigureRequest) {
        return calculatedDate(dateType, getProperty(dueDateProperties, DUE_DATE_TIME, isReconfigureRequest));
    }

    protected ConfigurationDmnEvaluationResponse calculatedDate(
        DateType dateType,
        ConfigurationDmnEvaluationResponse dueDateTimeResponse) {
        LocalDateTime dateTime = addTimeToDate(dueDateTimeResponse, DEFAULT_DATE);
        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }
}
