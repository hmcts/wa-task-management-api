package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskTypesDmnEvaluationResponse implements EvaluationResponse {
    private CamundaValue<String> taskTypeId;
    private CamundaValue<String> taskTypeName;

    private TaskTypesDmnEvaluationResponse() {
        //No-op constructor for deserialization
    }

    public TaskTypesDmnEvaluationResponse(CamundaValue<String> taskTypeId, CamundaValue<String> taskTypeName) {
        this.taskTypeId = taskTypeId;
        this.taskTypeName = taskTypeName;
    }

    public CamundaValue<String> getTaskTypeId() {
        return taskTypeId;
    }

    public CamundaValue<String> getTaskTypeName() {
        return taskTypeName;
    }

}
