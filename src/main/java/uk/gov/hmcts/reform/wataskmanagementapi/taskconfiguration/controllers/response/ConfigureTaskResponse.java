package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class ConfigureTaskResponse {
    private String taskId;
    private String caseId;
    private String assignee;
    private Map<String, Object> configurationVariables;

    private ConfigureTaskResponse() {
        //No-op constructor for deserialization
    }

    public ConfigureTaskResponse(String taskId,
                                 String caseId,
                                 String assignee,
                                 Map<String, Object> configurationVariables) {
        this.taskId = taskId;
        this.caseId = caseId;
        this.assignee = assignee;
        this.configurationVariables = configurationVariables;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getAssignee() {
        return assignee;
    }

    public Map<String, Object> getConfigurationVariables() {
        return configurationVariables;
    }
}
