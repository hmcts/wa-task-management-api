package uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Slf4j
public class TaskType {

    @Schema(name = "task_type_id")
    private final String taskTypeId;

    @Schema(name = "task_type_name")
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

        boolean isEqual = taskTypeId.equalsIgnoreCase(taskType.taskTypeId);

        if (isEqual) {
            log.info("Duplicate task type found for. {}", taskType);
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        return 31 * result + taskTypeId.toLowerCase(Locale.ROOT).hashCode();
    }

}
