package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class AddLocalVariableRequest {
    private final Map<String, CamundaValue<String>> modifications;

    public AddLocalVariableRequest(Map<String, CamundaValue<String>> modifications) {
        this.modifications = modifications;
    }

    public Map<String, CamundaValue<String>> getModifications() {
        return modifications;
    }
}
