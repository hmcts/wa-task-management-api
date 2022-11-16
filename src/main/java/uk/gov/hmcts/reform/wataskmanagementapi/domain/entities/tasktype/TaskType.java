package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskType taskType = (TaskType) o;
        return taskTypeId.equalsIgnoreCase(taskType.taskTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskTypeId);
    }

}
