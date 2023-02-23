package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoryVariableInstance {
    private String id;
    private String name;
    private String value;

    private HistoryVariableInstance() {
    }

    public HistoryVariableInstance(String id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

}
