package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaVariableInstance {
    private Object value;
    private String type;
    private String name;
    private String processInstanceId;

    private CamundaVariableInstance() {
        //Hidden constructor
    }

    public CamundaVariableInstance(Object value, String type, String name, String processInstanceId) {
        this.value = value;
        this.type = type;
        this.name = name;
        this.processInstanceId = processInstanceId;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }
}
