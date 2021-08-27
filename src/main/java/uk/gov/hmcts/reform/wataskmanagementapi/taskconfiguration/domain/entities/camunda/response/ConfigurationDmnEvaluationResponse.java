package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ConfigurationDmnEvaluationResponse implements EvaluationResponse {
    private final CamundaValue<String> name;
    private final CamundaValue<String> value;

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
