package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.Map;
import java.util.Objects;

public class AddLocalVariableRequest {
    private final Map<String, CamundaValue<String>> modifications;

    public AddLocalVariableRequest(Map<String, CamundaValue<String>> modifications) {
        this.modifications = modifications;
    }

    public Map<String, CamundaValue<String>> getModifications() {
        return modifications;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AddLocalVariableRequest that = (AddLocalVariableRequest) object;
        return Objects.equals(modifications, that.modifications);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifications);
    }

    @Override
    public String toString() {
        return "AddLocalVariableRequest{"
               + "modifications=" + modifications
               + '}';
    }
}
