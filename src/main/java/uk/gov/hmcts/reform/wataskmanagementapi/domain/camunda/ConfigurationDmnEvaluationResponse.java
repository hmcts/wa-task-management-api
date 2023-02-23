package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationDmnEvaluationResponse implements EvaluationResponse {
    private CamundaValue<String> name;
    private CamundaValue<String> value;
    @SuppressWarnings("PMD.LinguisticNaming")
    private CamundaValue<Boolean> canReconfigure;

    public ConfigurationDmnEvaluationResponse() {
        //No-op constructor for deserialization
    }

    public ConfigurationDmnEvaluationResponse(CamundaValue<String> name, CamundaValue<String> value) {
        this.name = name;
        this.value = value;
    }

    public ConfigurationDmnEvaluationResponse(CamundaValue<String> name,
                                              CamundaValue<String> value,
                                              CamundaValue<Boolean> canReconfigure
    ) {
        this.name = name;
        this.value = value;
        this.canReconfigure = canReconfigure;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }

    public CamundaValue<Boolean> getCanReconfigure() {
        return canReconfigure;
    }

    public void setName(CamundaValue<String> name) {
        this.name = name;
    }

    public void setValue(CamundaValue<String> value) {
        this.value = value;
    }

    public void setCanReconfigure(CamundaValue<Boolean> canReconfigure) {
        this.canReconfigure = canReconfigure;
    }
}
