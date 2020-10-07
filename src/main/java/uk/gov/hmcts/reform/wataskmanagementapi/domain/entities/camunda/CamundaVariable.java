package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaVariable {
    private String value;
    private String type;

    private CamundaVariable() {
        //Hidden constructor
    }

    public CamundaVariable(String value, String type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
