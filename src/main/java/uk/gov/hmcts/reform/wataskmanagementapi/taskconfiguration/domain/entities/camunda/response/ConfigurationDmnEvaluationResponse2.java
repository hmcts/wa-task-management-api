package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationDmnEvaluationResponse2 implements EvaluationResponse {
    private CamundaValue<String> name;
    private CamundaValue<String> value;
    private CamundaValue<String> canReConfigure;
    private CamundaValue<String> dummyProperty;

    public ConfigurationDmnEvaluationResponse2() {
        //No-op constructor for deserialization
    }

    public ConfigurationDmnEvaluationResponse2(CamundaValue<String> name, CamundaValue<String> value) {
        this.name = name;
        this.value = value;
    }

    public ConfigurationDmnEvaluationResponse2(CamundaValue<String> name, CamundaValue<String> value,
                                               CamundaValue<String> canReConfigure, CamundaValue<String> dummyProperty) {
        this.name = name;
        this.value = value;
        this.canReConfigure = canReConfigure;
        this.dummyProperty = dummyProperty;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }

    public CamundaValue<String> getCanReConfigure() {
        return canReConfigure;
    }

    public CamundaValue<String> getDummyProperty() {
        return dummyProperty;
    }
}
