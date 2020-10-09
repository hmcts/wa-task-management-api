package uk.gov.hmcts.reform.wataskmanagementapi.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryVariableInstance {
    private String name;
    private String value;

    private HistoryVariableInstance() {
    }

    public HistoryVariableInstance(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        HistoryVariableInstance that = (HistoryVariableInstance) object;
        return Objects.equals(name, that.name)
               && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "HistoryVariableInstance{"
               + "name='" + name + '\''
               + ", value='" + value + '\''
               + '}';
    }
}
