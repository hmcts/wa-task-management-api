package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;

import java.util.Map;

public class CreateTaskMessage {
    private final String messageName;
    private final Map<String, CamundaValue<?>> processVariables;

    public CreateTaskMessage(String messageName, Map<String, CamundaValue<?>> processVariables) {
        this.messageName = messageName;
        this.processVariables = processVariables;
    }

    public String getMessageName() {
        return messageName;
    }

    public Map<String, CamundaValue<?>> getProcessVariables() {
        return processVariables;
    }

    @JsonIgnore
    public String getCaseId() {
        CamundaValue<?> caseId = processVariables.get("caseId");
        return String.valueOf(caseId.getValue());
    }
}
