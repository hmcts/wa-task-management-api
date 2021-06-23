package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DmnRequest<T> {
    private final T variables;

    public DmnRequest(T variables) {
        this.variables = variables;
    }

    public T getVariables() {
        return variables;
    }

}
