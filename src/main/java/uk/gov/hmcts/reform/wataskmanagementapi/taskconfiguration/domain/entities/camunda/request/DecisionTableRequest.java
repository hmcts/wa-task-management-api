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
    private Map<String, Object> taskAttributes;

    private DecisionTableRequest() {
        //No-op constructor for deserialization
    }

    public DecisionTableRequest(CamundaValue<String> caseData, Map<String, Object> taskAttributes) {
        this.caseData = caseData;
        this.taskAttributes = taskAttributes;
    }

    @JsonProperty("caseData")
    public CamundaValue<String> getCaseData() {
        return caseData;
    }

    @JsonProperty("taskAttributes")
    public Map<String, Object> getTaskAttributes() {
        return taskAttributes;
    }
}
