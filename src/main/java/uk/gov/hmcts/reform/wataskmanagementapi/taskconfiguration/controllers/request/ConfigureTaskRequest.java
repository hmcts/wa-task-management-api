package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.request;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class ConfigureTaskRequest {
    private Map<String, Object> processVariables;

    private ConfigureTaskRequest() {
        //No-op constructor for deserialization
    }

    public ConfigureTaskRequest(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }
}
