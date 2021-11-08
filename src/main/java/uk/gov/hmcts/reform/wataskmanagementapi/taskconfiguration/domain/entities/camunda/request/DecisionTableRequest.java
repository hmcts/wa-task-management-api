package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@ToString
@EqualsAndHashCode
public class DecisionTableRequest {

    private CamundaValue<String> caseData;
    private CamundaValue<String> taskType;

    private DecisionTableRequest() {
        //No-op constructor for deserialization
    }

    public DecisionTableRequest(CamundaValue<String> caseData) {
        this.caseData = caseData;
    }

    public DecisionTableRequest(CamundaValue<String> caseData, CamundaValue<String> taskType) {
        this.caseData = caseData;
        this.taskType = taskType;
    }

    public CamundaValue<String> getCaseData() {
        return caseData;
    }

    public CamundaValue<String> getTaskType() {
        return taskType;
    }

}
