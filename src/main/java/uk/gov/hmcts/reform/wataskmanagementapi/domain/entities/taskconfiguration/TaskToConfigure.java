package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.taskconfiguration;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class TaskToConfigure {
    private final String id;
    private final String caseId;
    private final String name;
    private final Map<String, Object> processVariables;

    public TaskToConfigure(String id,
                           String caseId,
                           String name,
                           Map<String, Object> processVariables) {
        this.id = id;
        this.caseId = caseId;
        this.name = name;
        this.processVariables = processVariables;
    }

    public String getId() {
        return id;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }
}

