package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.tasktype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.Locale;

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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TaskType taskType = (TaskType) obj;
        return taskTypeId.equalsIgnoreCase(taskType.taskTypeId);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + taskTypeId.toLowerCase(Locale.ROOT).hashCode();
        return result;
    }

}
