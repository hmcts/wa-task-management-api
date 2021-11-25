package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@ToString
@EqualsAndHashCode
public class DecisionTableRequest {

    private CamundaValue<String> caseData;
    private CamundaValue<String> taskAttributes;

    private DecisionTableRequest() {
        //No-op constructor for deserialization
    }

    public DecisionTableRequest(CamundaValue<String> caseData, CamundaValue<String> taskAttributes) {
        this.caseData = caseData;
        this.taskAttributes = taskAttributes;
    }

    public CamundaValue<String> getCaseData() {
        return caseData;
    }

    public CamundaValue<String> getTaskAttributes() {
        return taskAttributes;
    }
}
