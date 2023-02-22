package uk.gov.hmcts.reform.wataskmanagementapi.domain.tasktype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.requireNonNull;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class TaskTypeResponse {

    private TaskType taskType;

    @JsonCreator
    public TaskTypeResponse(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskTypeResponse(String taskType) {
        requireNonNull(taskType);
        try {
            this.taskType = new ObjectMapper().reader()
                .forType(new TypeReference<TaskType>() {
                })
                .readValue(taskType);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Could not deserialize values");
        }
    }

    public TaskType getTaskType() {
        return taskType;
    }

    @JsonIgnore
    public String getTaskTypeAsJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(taskType);
    }
}
