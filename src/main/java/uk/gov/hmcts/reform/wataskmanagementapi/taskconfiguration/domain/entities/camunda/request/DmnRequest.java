package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DmnRequest<T> {
    private T variables;

    public DmnRequest(T variables) {
        this.variables = variables;
    }

    public T getVariables() {
        return variables;
    }

}
