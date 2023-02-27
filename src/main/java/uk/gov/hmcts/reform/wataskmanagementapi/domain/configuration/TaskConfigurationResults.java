package uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@EqualsAndHashCode
@ToString
public class TaskConfigurationResults {
    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse;
    private List<PermissionsDmnEvaluationResponse> permissionsDmnResponse;
    private Map<String, Object> processVariables;

    public TaskConfigurationResults(Map<String, Object> processVariables) {
        this(processVariables, emptyList(), emptyList());
    }

    public TaskConfigurationResults(Map<String, Object> processVariables,
                                    List<ConfigurationDmnEvaluationResponse> configurationDmnResponse,
                                    List<PermissionsDmnEvaluationResponse> permissionsDmnResponse) {
        this.processVariables = processVariables;
        this.configurationDmnResponse = configurationDmnResponse;
        this.permissionsDmnResponse = permissionsDmnResponse;
    }

    public List<ConfigurationDmnEvaluationResponse> getConfigurationDmnResponse() {
        return configurationDmnResponse;
    }

    public void setConfigurationDmnResponse(List<ConfigurationDmnEvaluationResponse> configurationDmnResponse) {
        this.configurationDmnResponse = configurationDmnResponse;
    }

    public List<PermissionsDmnEvaluationResponse> getPermissionsDmnResponse() {
        return permissionsDmnResponse;
    }

    public void setPermissionsDmnResponse(List<PermissionsDmnEvaluationResponse> permissionsDmnResponse) {
        this.permissionsDmnResponse = permissionsDmnResponse;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public void setProcessVariables(Map<String, Object> processVariables) {
        this.processVariables = processVariables;
    }
}

