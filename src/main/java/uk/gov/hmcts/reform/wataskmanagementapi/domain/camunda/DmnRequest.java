package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DmnRequest<T> {
    private T variables;

    public DmnRequest() {
        //No-op constructor for deserialization
    }

    public DmnRequest(T variables) {
        this.variables = variables;
    }

    public T getVariables() {
        return variables;
    }

}
