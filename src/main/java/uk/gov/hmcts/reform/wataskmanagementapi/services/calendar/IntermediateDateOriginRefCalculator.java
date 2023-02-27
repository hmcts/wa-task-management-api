package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateTypeConfigurator.DateTypeObject;

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
        DateTypeObject dateTypeObject,
        boolean isReconfigureRequest) {
        String dateTypeName = dateTypeObject.dateTypeName();
        ConfigurationDmnEvaluationResponse origin = getProperty(
            dueDateProperties,
            dateTypeName + ORIGIN_SUFFIX,
            isReconfigureRequest
        );
        ConfigurationDmnEvaluationResponse originRef = getProperty(
            dueDateProperties,
            dateTypeName + ORIGIN_REF_SUFFIX,
            isReconfigureRequest
        );
        return INTERMEDIATE_DATE == dateTypeObject.dateType()
            && Optional.ofNullable(origin).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, dateTypeName, isReconfigureRequest)).isEmpty()
            && Optional.ofNullable(originRef).isPresent();
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(String dateTypeName,
                                                       List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginRefDate(
            configResponses,
            getProperty(configResponses, dateTypeName + ORIGIN_REF_SUFFIX, isReconfigureRequest)
        );
    }
}
