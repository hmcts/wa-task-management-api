package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class TaskToConfigure {
    private final String id;
    private final String taskTypeId;
    private final String caseId;
    private final String name;

    public TaskToConfigure(String id,
                           String taskTypeId,
                           String caseId,
                           String name) {
        this.id = id;
        this.taskTypeId = taskTypeId;
        this.caseId = caseId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getTaskTypeId() {
        return taskTypeId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getName() {
        return name;
    }
}

