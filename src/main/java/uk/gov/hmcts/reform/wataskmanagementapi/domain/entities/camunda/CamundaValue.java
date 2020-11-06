package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.Objects;

public class CamundaValue<T> {
    private T value;
    private String type;

    private CamundaValue() {
        //Hidden constructor
    }

    public CamundaValue(T value, String type) {
        this.value = value;
        this.type = type;
    }

    public static CamundaValue<String> stringValue(String value) {
        return new CamundaValue<>(value, "String");
    }

    public static CamundaValue<Boolean> booleanValue(Boolean value) {
        return new CamundaValue<>(value, "boolean");
    }

    public static CamundaValue<String> jsonValue(String value) {
        return new CamundaValue<>(value, "json");
    }

    public T getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object anotherObject) {
        if (this == anotherObject) {
            return true;
        }
        if (anotherObject == null || getClass() != anotherObject.getClass()) {
            return false;
        }
        CamundaValue camundaValue = (CamundaValue) anotherObject;
        return Objects.equals(value, camundaValue.value)
               && Objects.equals(type, camundaValue.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return "CamundaValue{"
               + "value='" + value + '\''
               + ", type='" + type + '\''
               + '}';
    }
}
