package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class CompleteTaskVariables {
    private final Map<String, CamundaValue> variables;

    public CompleteTaskVariables() {
        variables = new HashMap<>();
    }

    public Map<String, CamundaValue> getVariables() {
        return variables;
    }
}
