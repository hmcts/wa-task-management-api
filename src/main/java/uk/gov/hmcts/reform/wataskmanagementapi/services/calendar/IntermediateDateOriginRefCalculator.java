package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.calendar.DateTypeIntervalData;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.INTERMEDIATE_DATE;

@Slf4j
@Component
public class IntermediateDateOriginRefCalculator extends IntermediateDateIntervalCalculator {

    public IntermediateDateOriginRefCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateTypeConfigurator.DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName();
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName + ORIGIN_SUFFIX)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName)).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName + ORIGIN_REF_SUFFIX)).isPresent()
            && !isReconfigureRequest;
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        DateTypeConfigurator.DateTypeObject dateTypeObject, List<ConfigurationDmnEvaluationResponse> configResponses) {
        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse originRefResponse
            = getProperty(configResponses, dateTypeName + ORIGIN_REF_SUFFIX);

        Optional<LocalDateTime> dueDateOriginRef = getOriginRefDate(configResponses, originRefResponse);
        DateTypeIntervalData dateTypeIntervalData = readDateTypeOriginFields(dateTypeName, configResponses, false);
        if (dueDateOriginRef.isPresent()) {
            dateTypeIntervalData = dateTypeIntervalData.toBuilder().calculatedRefDate(dueDateOriginRef.get()).build();
        }
        return calculateDate(dateTypeObject, dateTypeIntervalData);
    }
}
