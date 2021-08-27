package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@EqualsAndHashCode
@ToString
public class DecisionTableRequest {

    private CamundaValue<String> caseData;

    private DecisionTableRequest() {
    }

    public DecisionTableRequest(CamundaValue<String> caseData) {
        this.caseData = caseData;
    }

    @JsonProperty("caseData")
    public CamundaValue<String> getCaseData() {
        return caseData;
    }
}
