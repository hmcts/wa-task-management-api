package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class CamundaSendMessageRequest {

    private final String messageName;
    private final Map<String, CamundaValue<?>> processVariables;

    public CamundaSendMessageRequest(String messageName, Map<String, CamundaValue<?>> processVariables) {
        this.messageName = messageName;
        this.processVariables = processVariables;
    }

    public String getMessageName() {
        return messageName;
    }

    public Map<String, CamundaValue<?>> getProcessVariables() {
        return processVariables;
    }

}

