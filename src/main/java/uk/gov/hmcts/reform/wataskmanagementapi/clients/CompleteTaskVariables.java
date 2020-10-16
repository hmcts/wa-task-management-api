package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CompleteTaskVariables {
    private final Map<String, CamundaValue> variables;

    public CompleteTaskVariables() {
        variables = new HashMap<>();
    }

    public Map<String, CamundaValue> getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CompleteTaskVariables that = (CompleteTaskVariables) object;
        return Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variables);
    }

    @Override
    public String toString() {
        return "CompleteTaskVariables{"
               + "variables=" + variables
               + '}';
    }
}
