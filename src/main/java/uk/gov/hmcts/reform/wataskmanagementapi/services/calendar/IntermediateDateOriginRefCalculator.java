package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse origin = getProperty(
            configResponses,
            dateTypeName + ORIGIN_SUFFIX,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse originRef = getProperty(
            configResponses,
            dateTypeName + ORIGIN_REF_SUFFIX,
            //always intermediate date values will be read hence isReconfigurableRequest value is set to false
            false
        );
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(origin).isEmpty()
            && isPropertyEmptyIrrespectiveOfReconfiguration(configResponses, dateTypeName)
            && Optional.ofNullable(originRef).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(
        String dateTypeName,
        List<ConfigurationDmnEvaluationResponse> configResponses,
        boolean isReconfigureRequest,
        Map<String, Object> taskAttributes,
        List<ConfigurationDmnEvaluationResponse> calculatedConfigurations) {
        var configProperty = getProperty(
            configResponses,
            dateTypeName + ORIGIN_REF_SUFFIX,
            //always intermediate date values will be read hence isReconfigurableRequest value is set to false
            false
        );
        log.info("Input {}: {}", dateTypeName + ORIGIN_REF_SUFFIX, configProperty);
        return getOriginRefDate(calculatedConfigurations, configProperty, taskAttributes, isReconfigureRequest);
    }
}
