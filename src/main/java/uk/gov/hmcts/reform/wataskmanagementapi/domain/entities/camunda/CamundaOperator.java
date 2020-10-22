package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.annotation.JsonValue;

import static java.util.Arrays.stream;

public enum CamundaOperator {

    EQUAL("eq"),
    NOT_EQUAL("neq"),
    GREATER_THAN("gt"),
    GREATER_THAN_OR_EQUAL("gteq"),
    LOWER_THAN("lt"),
    LOWER_THAN_OR_EQUAL("lteq"),
    LIKE("like");

    @JsonValue
    private String value;

    CamundaOperator(String value) {
        this.value = value;
    }

    public static CamundaOperator from(String value) {
        return stream(values())
            .filter(v -> v.getValue().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(value + " is an unsupported operator"));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
