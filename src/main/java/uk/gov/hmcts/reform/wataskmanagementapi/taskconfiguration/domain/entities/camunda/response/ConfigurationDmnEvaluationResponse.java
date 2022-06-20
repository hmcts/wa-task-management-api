package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

import java.util.Map;

@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationDmnEvaluationResponse implements EvaluationResponse {
    private CamundaValue<String> name;
    private CamundaValue<String> value;

    public ConfigurationDmnEvaluationResponse() {
        //No-op constructor for deserialization
    }

    public ConfigurationDmnEvaluationResponse(CamundaValue<String> name, CamundaValue<String> value) {
        this.name = name;
        this.value = value;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }
}
