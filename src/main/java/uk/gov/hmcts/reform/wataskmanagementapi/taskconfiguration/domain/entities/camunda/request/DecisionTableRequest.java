package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

import java.util.Map;

@EqualsAndHashCode
@ToString
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

    @JsonProperty("caseData")
    public CamundaValue<String> getCaseData() {
        return caseData;
    }

    @JsonProperty("taskAttributes")
    public CamundaValue<String> getTaskAttributes() {
        return taskAttributes;
    }
}
