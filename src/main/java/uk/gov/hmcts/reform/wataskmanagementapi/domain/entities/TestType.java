package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum TestType {

    YES("yes"),
    NO("no");

    @JsonValue
    private final String value;

    TestType(String value) {
        this.value = value;
    }

    public static Optional<TestType> from(
        String value
    ) {
        return stream(values())
            .filter(v -> v.getValue().equals(value))
            .findFirst();
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

}
