package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@EqualsAndHashCode
public class TaskType {

    private final String taskTypeId;

    private final String taskTypeName;

    @JsonCreator
    public TaskType(@JsonProperty("task_type_id") String taskTypeId,
                    @JsonProperty("task_type_name") String taskTypeName) {
        this.taskTypeId = taskTypeId;
        this.taskTypeName = taskTypeName;
    }

    public String getTaskTypeId() {
        return taskTypeId;
    }

    public String getTaskTypeName() {
        return taskTypeName;
    }

}
