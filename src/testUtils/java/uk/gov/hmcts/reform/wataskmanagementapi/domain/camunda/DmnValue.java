package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class DmnValue<T> {
    private T value;
    private String type;

    private DmnValue() {
    }

    public DmnValue(T value, String type) {
        this.value = value;
        this.type = type;
    }

    public static DmnValue<Boolean> dmnBooleanValue(boolean value) {
        return new DmnValue<>(value, "Boolean");
    }

    public static DmnValue<String> dmnStringValue(String value) {
        return new DmnValue<>(value, "String");
    }

    public static DmnValue<Map<String, Object>> dmnMapValue(Map<String, Object> value) {
        return new DmnValue<>(value, null);
    }

    public static DmnValue<Integer> dmnIntegerValue(Integer value) {
        return new DmnValue<>(value, "Integer");
    }

    public T getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

}
