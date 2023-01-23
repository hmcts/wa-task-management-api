package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.DUE_DATE;

@Slf4j
@Component
public class DueDateIntervalReCalculator extends DueDateIntervalCalculator {

    public DueDateIntervalReCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        var dueDateOrigin = getReConfigurableProperty(dueDateProperties, DUE_DATE_ORIGIN);
        var dueDate = getReConfigurableProperty(dueDateProperties, DUE_DATE.getType());
        return DUE_DATE == dateType
            && Optional.ofNullable(dueDateOrigin).isPresent()
            && Optional.ofNullable(dueDate).isEmpty()
            && isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties,
                                                            DateType dateType) {
        DateTypeIntervalData dateTypeIntervalData = readDueDateOriginFields(dueDateProperties, true);
        LocalDateTime dueDate = readDueDateOrigin(dueDateProperties, true);

        LocalDateTime dateTime = calculate(dateTypeIntervalData, dueDate);

        return ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue(dateType.getType()))
            .value(CamundaValue.stringValue(dateType.getDateTimeFormatter().format(dateTime)))
            .build();
    }


}
