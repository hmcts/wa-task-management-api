package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
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

    public static CamundaValue<Integer> integerValue(Integer value) {
        return new CamundaValue<>(value, "Integer");
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

}
