package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaVariableInstance {
    private Object value;
    private String type;
    private String name;
    private String processInstanceId;
    private String taskId;

    private CamundaVariableInstance() {
        //Hidden constructor
    }

    public CamundaVariableInstance(Object value,
                                   String type,
                                   String name,
                                   String processInstanceId,
                                   String taskId) {
        this.value = value;
        this.type = type;
        this.name = name;
        this.processInstanceId = processInstanceId;
        this.taskId = taskId;
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

    public String getTaskId() {
        return taskId;
    }
}
